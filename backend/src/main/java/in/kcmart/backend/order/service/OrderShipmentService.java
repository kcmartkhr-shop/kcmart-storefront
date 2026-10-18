package in.kcmart.backend.order.service;

import in.kcmart.backend.order.dto.admin.AdminOrderShipmentResponse;
import in.kcmart.backend.order.dto.admin.AdminOrderShipmentUpsertRequest;
import in.kcmart.backend.order.entity.Order;
import in.kcmart.backend.order.entity.OrderShipment;
import in.kcmart.backend.order.repository.OrderRepository;
import in.kcmart.backend.order.repository.OrderShipmentRepository;
import in.kcmart.backend.shipping.delhivery.repository.DelhiveryShipmentBookingAttemptRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.time.OffsetDateTime;

@Service
public class OrderShipmentService {

    private final OrderRepository orders;
    private final OrderShipmentRepository shipments;
    private final DelhiveryShipmentBookingAttemptRepository bookingAttempts;

    public OrderShipmentService(
            OrderRepository orders,
            OrderShipmentRepository shipments,
            DelhiveryShipmentBookingAttemptRepository bookingAttempts) {

        this.orders = orders;
        this.shipments = shipments;
        this.bookingAttempts = bookingAttempts;
    }

    @Transactional(readOnly = true)
    public AdminOrderShipmentResponse getByOrderId(Long orderId) {

        Order order = orders.findById(orderId)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Order not found."
            ));

        requireDelivery(order);

        OrderShipment shipment = shipments.findByOrderId(orderId)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Shipment not found for this order."
            ));

        return toResponse(shipment);
    }

    @Transactional
    public AdminOrderShipmentResponse upsert(
            Long orderId,
            AdminOrderShipmentUpsertRequest request,
            String actorIdentifier) {

        Order order = orders.findByIdForUpdate(orderId)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Order not found."
            ));

        requireDelivery(order);

        if ("COMPLETED".equals(normalize(order.getOrderStatus()))) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Shipping details cannot be modified after the order is completed."
            );
        }

        if (request == null) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Shipment request is required."
            );
        }

        if (bookingAttempts.existsByOrderId(orderId)) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Manual shipment changes are blocked because a Delhivery booking attempt exists."
            );
        }
        String carrierName = required(
            request.carrierName(),
            "Carrier name",
            100
        );

        String trackingNumber = required(
            request.trackingNumber(),
            "Tracking number",
            150
        );

        String trackingUrl = optional(
            request.trackingUrl(),
            "Tracking URL",
            1000
        );

        String adminNote = optional(
            request.adminNote(),
            "Admin note",
            1000
        );

        if (trackingUrl != null) {
            validateTrackingUrl(trackingUrl);
        }

        String actor = normalizeActor(actorIdentifier);

        OrderShipment shipment = shipments.findByOrderId(orderId)
            .orElse(null);

        if (shipment == null) {
            shipment = new OrderShipment();
            shipment.setOrderId(orderId);
            shipment.setShipmentStatus(initialShipmentStatus(order));
            shipment.setCreatedBy(actor);
        }

        shipment.setCarrierName(carrierName);
        shipment.setTrackingNumber(trackingNumber);
        shipment.setTrackingUrl(trackingUrl);
        shipment.setAdminNote(adminNote);
        shipment.setUpdatedBy(actor);
        shipment.setUpdatedAt(OffsetDateTime.now());

        OrderShipment saved = shipments.save(shipment);

        return toResponse(saved);
    }

    private void requireDelivery(Order order) {

        String fulfillmentType = normalize(order.getFulfillmentType());

        if (!"DELIVERY".equals(fulfillmentType)) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Shipment information is available only for DELIVERY orders."
            );
        }
    }

    private String initialShipmentStatus(Order order) {

        String orderStatus = normalize(order.getOrderStatus());

        if ("PACKED".equals(orderStatus)) {
            return "PACKED";
        }

        if ("SHIPPED".equals(orderStatus)) {
            return "SHIPPED";
        }

        if ("OUT_FOR_DELIVERY".equals(orderStatus)) {
            return "OUT_FOR_DELIVERY";
        }

        if ("DELIVERED".equals(orderStatus)
                || "COMPLETED".equals(orderStatus)) {
            return "DELIVERED";
        }

        return "PENDING";
    }

    private void validateTrackingUrl(String value) {

        try {
            URI uri = URI.create(value);

            String scheme = uri.getScheme();

            if (scheme == null
                    || (!"https".equalsIgnoreCase(scheme)
                    && !"http".equalsIgnoreCase(scheme))
                    || uri.getHost() == null
                    || uri.getHost().isBlank()) {

                throw new IllegalArgumentException();
            }

        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Tracking URL must be a valid http or https URL."
            );
        }
    }

    private String required(
            String value,
            String fieldName,
            int maxLength) {

        if (value == null || value.isBlank()) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                fieldName + " is required."
            );
        }

        String normalized = value.trim();

        if (normalized.length() > maxLength) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                fieldName + " must not exceed "
                    + maxLength
                    + " characters."
            );
        }

        return normalized;
    }

    private String optional(
            String value,
            String fieldName,
            int maxLength) {

        if (value == null || value.isBlank()) {
            return null;
        }

        String normalized = value.trim();

        if (normalized.length() > maxLength) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                fieldName + " must not exceed "
                    + maxLength
                    + " characters."
            );
        }

        return normalized;
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }

        return value.trim().toUpperCase();
    }

    private String normalizeActor(String value) {

        if (value == null || value.isBlank()) {
            return "admin";
        }

        String normalized = value.trim();

        if (normalized.length() > 100) {
            return normalized.substring(0, 100);
        }

        return normalized;
    }

    private AdminOrderShipmentResponse toResponse(
            OrderShipment shipment) {

        return new AdminOrderShipmentResponse(
            shipment.getId(),
            shipment.getOrderId(),
            shipment.getCarrierName(),
            shipment.getTrackingNumber(),
            shipment.getTrackingUrl(),
            shipment.getShipmentStatus(),
            shipment.getShippedAt(),
            shipment.getOutForDeliveryAt(),
            shipment.getDeliveredAt(),
            shipment.getAdminNote(),
            shipment.getCreatedBy(),
            shipment.getUpdatedBy(),
            shipment.getCreatedAt(),
            shipment.getUpdatedAt()
        );
    }
}