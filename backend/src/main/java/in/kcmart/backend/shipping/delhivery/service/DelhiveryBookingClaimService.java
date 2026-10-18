package in.kcmart.backend.shipping.delhivery.service;

import in.kcmart.backend.order.entity.Order;
import in.kcmart.backend.order.repository.OrderRepository;
import in.kcmart.backend.order.repository.OrderShipmentRepository;
import in.kcmart.backend.shipping.delhivery.config.DelhiveryProperties;
import in.kcmart.backend.shipping.delhivery.entity.DelhiveryShipmentBookingAttempt;
import in.kcmart.backend.shipping.delhivery.repository.DelhiveryShipmentBookingAttemptRepository;
import in.kcmart.backend.shipping.delhivery.repository.DelhiveryShipmentPackageRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Locale;

@Service
public class DelhiveryBookingClaimService {

    private final OrderRepository orders;
    private final OrderShipmentRepository shipments;
    private final DelhiveryShipmentBookingAttemptRepository attempts;
    private final DelhiveryProperties properties;
    private final DelhiveryShipmentPackageRepository packages;
    private final DelhiveryBookingEligibilityValidator validator;

    @Autowired
    public DelhiveryBookingClaimService(
            OrderRepository orders,
            OrderShipmentRepository shipments,
            DelhiveryShipmentBookingAttemptRepository attempts,
            DelhiveryProperties properties,
            DelhiveryShipmentPackageRepository packages,
            DelhiveryBookingEligibilityValidator validator) {

        this.orders = orders;
        this.shipments = shipments;
        this.attempts = attempts;
        this.properties = properties;
        this.packages = packages;
        this.validator = validator;
    }
    @Transactional
    public Long claim(Long orderId) {

        if (!properties.isShipmentCreationEnabled()) {
            throw new ResponseStatusException(
                HttpStatus.SERVICE_UNAVAILABLE,
                "Delhivery shipment creation is disabled."
            );
        }

        if (orderId == null || orderId <= 0) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Valid order ID is required."
            );
        }

        Order order = orders.findByIdForUpdate(orderId)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Order not found."
            ));

        if (!"DELIVERY".equals(normalize(order.getFulfillmentType()))) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Only DELIVERY orders can be booked."
            );
        }

        String status = normalize(order.getOrderStatus());

        if (!"PACKED".equals(status)) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Order must be PACKED before Delhivery booking."
            );
        }

        if (shipments.findByOrderIdForUpdate(orderId)
                .filter(shipment ->
                    !"PACKED".equals(normalize(shipment.getShipmentStatus()))
                    || (shipment.getCarrierName() != null
                        && !shipment.getCarrierName().isBlank())
                    || (shipment.getTrackingNumber() != null
                        && !shipment.getTrackingNumber().isBlank())
                )
                .isPresent()) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Shipment already exists for this order."
            );
        }

        if (attempts.existsByOrderId(orderId)) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Delhivery booking attempt already exists. Reconciliation required."
            );
        }

        var shipmentPackage = packages.findByOrderId(orderId)
            .orElse(null);

        validator.validate(order, shipmentPackage);
        DelhiveryShipmentBookingAttempt attempt =
            new DelhiveryShipmentBookingAttempt(orderId);

        return attempts.saveAndFlush(attempt).getId();
    }

    private String normalize(String value) {
        return value == null
            ? ""
            : value.trim().toUpperCase(Locale.ROOT);
    }
}