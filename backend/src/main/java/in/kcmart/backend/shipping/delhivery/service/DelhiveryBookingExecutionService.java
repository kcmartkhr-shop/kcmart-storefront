package in.kcmart.backend.shipping.delhivery.service;

import in.kcmart.backend.order.entity.Order;
import in.kcmart.backend.order.repository.OrderRepository;
import in.kcmart.backend.order.repository.OrderItemRepository;
import in.kcmart.backend.shipping.delhivery.client.DelhiveryClient;
import in.kcmart.backend.shipping.delhivery.config.DelhiveryProperties;
import in.kcmart.backend.shipping.delhivery.repository.DelhiveryShipmentPackageRepository;
import in.kcmart.backend.shipping.delhivery.repository.DelhiveryShipmentBookingAttemptRepository;

import org.springframework.stereotype.Service;

@Service
public class DelhiveryBookingExecutionService {

    private final OrderRepository orders;
    private final OrderItemRepository items;
    private final DelhiveryShipmentPackageRepository packages;
    private final DelhiveryShipmentBookingAttemptRepository attempts;
    private final DelhiveryShipmentPayloadBuilder payloadBuilder;
    private final DelhiveryShipmentResponseValidator responseValidator;
    private final DelhiveryBookingConfirmationService confirmation;
    private final DelhiveryBookingResultService results;
    private final DelhiveryBookingExecutionClaimService executionClaim;
    private final DelhiveryClient client;
    private final DelhiveryProperties properties;

    public DelhiveryBookingExecutionService(
            OrderRepository orders,
            OrderItemRepository items,
            DelhiveryShipmentPackageRepository packages,
            DelhiveryShipmentBookingAttemptRepository attempts,
            DelhiveryShipmentPayloadBuilder payloadBuilder,
            DelhiveryShipmentResponseValidator responseValidator,
            DelhiveryBookingConfirmationService confirmation,
            DelhiveryBookingResultService results,
            DelhiveryBookingExecutionClaimService executionClaim,
            DelhiveryClient client,
            DelhiveryProperties properties) {

        this.orders = orders;
        this.items = items;
        this.packages = packages;
        this.attempts = attempts;
        this.payloadBuilder = payloadBuilder;
        this.responseValidator = responseValidator;
        this.confirmation = confirmation;
        this.results = results;
        this.executionClaim = executionClaim;
        this.client = client;
        this.properties = properties;
    }

    public String execute(Long orderId) {

        if (!properties.isShipmentCreationEnabled()) {
            throw new IllegalStateException(
                "Delhivery shipment creation is disabled."
            );
        }

        if (orderId == null || orderId <= 0) {
            throw new IllegalArgumentException("Valid order ID required.");
        }

        var attempt = attempts.findByOrderId(orderId)
            .orElseThrow(() -> new IllegalStateException(
                "Booking attempt not found."
            ));

        if (!"PENDING".equals(attempt.getAttemptStatus())) {
            throw new IllegalStateException(
                "Booking attempt requires reconciliation."
            );
        }

        Order order = orders.findById(orderId)
            .orElseThrow(() -> new IllegalStateException(
                "Order not found."
            ));

        var shipmentPackage = packages.findByOrderId(orderId)
            .orElseThrow(() -> new IllegalStateException(
                "Shipment package not found."
            ));

        // Build and validate everything before making an external request.
        String payload = payloadBuilder.build(
            order,
            shipmentPackage,
            items.findByOrderIdOrderByIdAsc(orderId),
            properties.getPickupLocation()
        );

        // Commit the exclusive claim before any external API request.
        // A PROCESSING attempt must never be automatically retried.
        executionClaim.claim(orderId);

        String response;

        try {
            response = client.createShipment(payload);
        } catch (Exception ex) {
            results.recordUnknown(
                orderId,
                "Shipment request outcome uncertain; manual reconciliation required."
            );
            throw new IllegalStateException(
                "Delhivery booking outcome is uncertain. Do not retry automatically.",
                ex
            );
        }

        String waybill;

        try {
            waybill = responseValidator.extractWaybill(response);
        } catch (Exception ex) {
            results.recordUnknown(
                orderId,
                "Shipment response could not be confirmed; manual reconciliation required."
            );
            throw new IllegalStateException(
                "Delhivery response requires manual reconciliation.",
                ex
            );
        }

        try {
            confirmation.confirm(orderId, waybill);
        } catch (Exception ex) {
            try {
                results.recordUnknown(
                    orderId,
                    "AWB received but local confirmation failed; manual reconciliation required."
                );
            } catch (Exception recordingFailure) {
                ex.addSuppressed(recordingFailure);
            }

            throw new IllegalStateException(
                "AWB received but local confirmation failed. Do not book again.",
                ex
            );
        }

        return waybill;
    }
}