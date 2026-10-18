package in.kcmart.backend.order.service;

import in.kcmart.backend.order.entity.Order;
import in.kcmart.backend.order.entity.OrderNotification;
import in.kcmart.backend.order.entity.OrderShipment;
import in.kcmart.backend.order.event.OrderCompletionNotificationEvent;
import in.kcmart.backend.order.event.OrderShipmentNotificationEvent;
import in.kcmart.backend.order.entity.OrderStatusHistory;
import in.kcmart.backend.order.repository.OrderInvoiceRepository;
import in.kcmart.backend.order.repository.OrderNotificationRepository;
import in.kcmart.backend.order.repository.OrderRepository;
import in.kcmart.backend.order.repository.OrderStatusHistoryRepository;
import in.kcmart.backend.order.repository.OrderShipmentRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Set;

@Service
public class OrderLifecycleService {

    private static final Map<String, Set<String>> STORE_PICKUP_TRANSITIONS =
        Map.of(
            "PENDING", Set.of("CONFIRMED"),
            "CONFIRMED", Set.of("READY_FOR_PICKUP"),
            "READY_FOR_PICKUP", Set.of("COMPLETED")
        );

    private static final Map<String, Set<String>> DELIVERY_TRANSITIONS =
        Map.of(
            "PENDING", Set.of("CONFIRMED"),
            "CONFIRMED", Set.of("PROCESSING"),
            "PROCESSING", Set.of("PACKED"),
            "PACKED", Set.of("SHIPPED"),
            "SHIPPED", Set.of("OUT_FOR_DELIVERY"),
            "OUT_FOR_DELIVERY", Set.of("DELIVERED"),
            "DELIVERED", Set.of("COMPLETED")
        );

    private final OrderRepository orders;
    private final OrderStatusHistoryRepository history;
    private final OrderInvoiceRepository invoices;
    private final OrderNotificationRepository notifications;
    private final OrderShipmentRepository shipments;
    private final ApplicationEventPublisher eventPublisher;

    public OrderLifecycleService(
            OrderRepository orders,
            OrderStatusHistoryRepository history,
            OrderInvoiceRepository invoices,
            OrderNotificationRepository notifications,
            OrderShipmentRepository shipments,
            ApplicationEventPublisher eventPublisher) {

        this.orders = orders;
        this.history = history;
        this.invoices = invoices;
        this.notifications = notifications;
        this.shipments = shipments;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public Order transition(
            Long orderId,
            String requestedStatus,
            String actorIdentifier,
            String note) {

        Order order = orders.findByIdForUpdate(orderId)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Order not found."
            ));

        String currentStatus = normalize(order.getOrderStatus());
        String newStatus = normalize(requestedStatus);
        String fulfillmentType = normalize(order.getFulfillmentType());

        if (newStatus == null || newStatus.isBlank()) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "New order status is required."
            );
        }

        if (currentStatus.equals(newStatus)) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Order is already in status " + currentStatus + "."
            );
        }

        Map<String, Set<String>> allowedTransitions;

        if ("STORE_PICKUP".equals(fulfillmentType)) {
            allowedTransitions = STORE_PICKUP_TRANSITIONS;
        } else if ("DELIVERY".equals(fulfillmentType)) {
            allowedTransitions = DELIVERY_TRANSITIONS;
        } else {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Unsupported fulfillment type: " + fulfillmentType
            );
        }

        Set<String> allowedNextStatuses =
            allowedTransitions.getOrDefault(
                currentStatus,
                Set.of()
            );

        if (!allowedNextStatuses.contains(newStatus)) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Invalid order status transition: "
                    + currentStatus
                    + " -> "
                    + newStatus
                    + " for "
                    + fulfillmentType
                    + "."
            );
        }

        /*
         * Delivery payment safety:
         *
         * A DELIVERY order may enter the fulfillment lifecycle only
         * after its ONLINE payment has been recorded as PAID.
         *
         * This prevents an unpaid delivery order from moving
         * PENDING -> CONFIRMED.
         *
         * Payment state must be established by a trusted payment
         * flow, never by this lifecycle transition.
         *
         * This guard performs no inventory operation.
         */
        if ("DELIVERY".equals(fulfillmentType)
                && "PENDING".equals(currentStatus)
                && "CONFIRMED".equals(newStatus)) {

            if (!"ONLINE".equals(
                    normalize(order.getPaymentMethod()))) {

                throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Delivery confirmation requires ONLINE payment."
                );
            }

            if (!"PAID".equals(
                    normalize(order.getPaymentStatus()))) {

                throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Delivery order cannot be confirmed until payment is PAID."
                );
            }
        }

        /*
         * Store Pickup completion safety:
         *
         * READY_FOR_PICKUP -> COMPLETED is allowed only when:
         * 1. Payment has actually been recorded as PAID.
         * 2. An invoice exists for the order.
         *
         * This guard performs no inventory operation.
         */
        if ("STORE_PICKUP".equals(fulfillmentType)
                && "COMPLETED".equals(newStatus)) {

            String paymentStatus =
                normalize(order.getPaymentStatus());

            if (!"PAID".equals(paymentStatus)) {
                throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Store pickup order cannot be completed until payment is PAID."
                );
            }

            if (!invoices.existsByOrderId(order.getId())) {
                throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Store pickup order cannot be completed until an invoice exists."
                );
            }
        }

        /*
         * Delivery completion safety:
         *
         * DELIVERED -> COMPLETED requires:
         * 1. ONLINE payment method.
         * 2. Payment recorded as PAID.
         * 3. Shipment already DELIVERED.
         * 4. Invoice exists.
         *
         * This guard performs no inventory operation.
         */
        if ("DELIVERY".equals(fulfillmentType)
                && "COMPLETED".equals(newStatus)) {

            if (!"ONLINE".equals(
                    normalize(order.getPaymentMethod()))) {

                throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Delivery completion requires ONLINE payment."
                );
            }

            if (!"PAID".equals(
                    normalize(order.getPaymentStatus()))) {

                throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Delivery order cannot be completed until payment is PAID."
                );
            }

            OrderShipment deliveredShipment = shipments
                .findByOrderIdForUpdate(order.getId())
                .orElseThrow(() ->
                    new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Delivery order cannot be completed without shipment information."
                    )
                );

            if (!"DELIVERED".equals(
                    normalize(deliveredShipment.getShipmentStatus()))) {

                throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Shipment must be DELIVERED before the order can be completed."
                );
            }

            if (!invoices.existsByOrderId(order.getId())) {
                throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Delivery order cannot be completed until an invoice exists."
                );
            }
        }

        /*
         * Shipping safety:
         *
         * A delivery order cannot enter SHIPPED state until
         * its invoice has been uploaded.
         *
         * The shipment notification includes this invoice,
         * so the order must not commit SHIPPED without it.
         *
         * This guard performs no inventory or payment mutation.
         */
        if ("DELIVERY".equals(fulfillmentType)
                && "PACKED".equals(currentStatus)
                && "SHIPPED".equals(newStatus)
                && !invoices.existsByOrderId(order.getId())) {

            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Delivery order cannot be shipped until an invoice exists."
            );
        }

        if ("DELIVERY".equals(fulfillmentType)) {
            synchronizeShipment(
                order,
                newStatus,
                actorIdentifier
            );
        }

        order.setOrderStatus(newStatus);
        Order savedOrder = orders.save(order);

        OrderStatusHistory event = new OrderStatusHistory();
        event.setOrderId(savedOrder.getId());
        event.setPreviousStatus(currentStatus);
        event.setNewStatus(newStatus);
        event.setActorType("ADMIN");
        event.setActorIdentifier(
            normalizeActor(actorIdentifier)
        );
        event.setNote(normalizeNote(note));

        history.save(event);

        if ("DELIVERY".equals(fulfillmentType)
                && "SHIPPED".equals(newStatus)) {
            createShipmentNotification(savedOrder);
        }

        if ("COMPLETED".equals(newStatus)) {
            createCompletionNotification(savedOrder);
        }

        return savedOrder;
    }

    private void synchronizeShipment(
            Order order,
            String newOrderStatus,
            String actorIdentifier) {

        if (!Set.of(
                "PACKED",
                "SHIPPED",
                "OUT_FOR_DELIVERY",
                "DELIVERED"
            ).contains(newOrderStatus)) {

            return;
        }

        OrderShipment shipment = shipments
            .findByOrderIdForUpdate(order.getId())
            .orElse(null);

        /*
         * A shipment record is not mandatory while the order is merely
         * being processed.
         *
         * When PROCESSING -> PACKED occurs, create a shipment shell so
         * the admin can enter carrier/tracking information before SHIPPED.
         */
        if ("PACKED".equals(newOrderStatus)) {

            if (shipment == null) {
                shipment = new OrderShipment();
                shipment.setOrderId(order.getId());
                shipment.setShipmentStatus("PACKED");
                shipment.setCreatedBy(
                    normalizeActor(actorIdentifier)
                );
            } else {
                shipment.setShipmentStatus("PACKED");
            }

            shipment.setUpdatedBy(
                normalizeActor(actorIdentifier)
            );
            shipment.setUpdatedAt(OffsetDateTime.now());

            shipments.save(shipment);
            return;
        }

        /*
         * SHIPPED and later states require an existing shipment.
         */
        if (shipment == null) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Shipment information must exist before the order can be marked "
                    + newOrderStatus
                    + "."
            );
        }

        /*
         * An order must never be marked SHIPPED without the minimum
         * tracking information required by the admin workflow.
         */
        if ("SHIPPED".equals(newOrderStatus)) {

            if (shipment.getCarrierName() == null
                    || shipment.getCarrierName().isBlank()
                    || shipment.getTrackingNumber() == null
                    || shipment.getTrackingNumber().isBlank()) {

                throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Carrier name and tracking number are required before shipping."
                );
            }

            shipment.setShipmentStatus("SHIPPED");

            if (shipment.getShippedAt() == null) {
                shipment.setShippedAt(OffsetDateTime.now());
            }
        }

        if ("OUT_FOR_DELIVERY".equals(newOrderStatus)) {

            if (!"SHIPPED".equals(
                    normalize(shipment.getShipmentStatus()))) {

                throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Shipment must be SHIPPED before OUT_FOR_DELIVERY."
                );
            }

            shipment.setShipmentStatus("OUT_FOR_DELIVERY");

            if (shipment.getOutForDeliveryAt() == null) {
                shipment.setOutForDeliveryAt(
                    OffsetDateTime.now()
                );
            }
        }

        if ("DELIVERED".equals(newOrderStatus)) {

            if (!"OUT_FOR_DELIVERY".equals(
                    normalize(shipment.getShipmentStatus()))) {

                throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Shipment must be OUT_FOR_DELIVERY before DELIVERED."
                );
            }

            shipment.setShipmentStatus("DELIVERED");

            if (shipment.getDeliveredAt() == null) {
                shipment.setDeliveredAt(
                    OffsetDateTime.now()
                );
            }
        }

        shipment.setUpdatedBy(
            normalizeActor(actorIdentifier)
        );
        shipment.setUpdatedAt(OffsetDateTime.now());

        shipments.save(shipment);
    }
    private void createShipmentNotification(Order order) {

        OrderNotification notification = notifications
            .findByOrderIdAndNotificationTypeAndChannel(
                order.getId(),
                "ORDER_SHIPPED",
                "EMAIL"
            )
            .orElse(null);

        if (notification != null) {

            if (!order.getCustomerEmail().equalsIgnoreCase(
                    notification.getRecipient())) {

                throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Existing shipment notification recipient does not match order email."
                );
            }

            eventPublisher.publishEvent(
                new OrderShipmentNotificationEvent(
                    notification.getId()
                )
            );

            return;
        }

        OrderNotification created =
            new OrderNotification();

        created.setOrderId(order.getId());
        created.setNotificationType("ORDER_SHIPPED");
        created.setChannel("EMAIL");
        created.setRecipient(order.getCustomerEmail());

        created.setSubject(
            "Your KCMART order has been shipped - "
                + order.getOrderNumber()
        );

        created.setStatus("PENDING");
        created.setAttemptCount(0);
        created.setLastError(null);
        created.setSentAt(null);

        OrderNotification saved =
            notifications.save(created);

        eventPublisher.publishEvent(
            new OrderShipmentNotificationEvent(
                saved.getId()
            )
        );
    }

    private void createCompletionNotification(Order order) {

        OrderNotification notification = notifications
            .findByOrderIdAndNotificationTypeAndChannel(
                order.getId(),
                "ORDER_COMPLETED",
                "EMAIL"
            )
            .orElse(null);

        if (notification != null) {

            if (!order.getCustomerEmail().equalsIgnoreCase(
                    notification.getRecipient())) {
                throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Existing completion notification recipient does not match order email."
                );
            }

            eventPublisher.publishEvent(
                new OrderCompletionNotificationEvent(
                    notification.getId()
                )
            );

            return;
        }

        OrderNotification created = new OrderNotification();
        created.setOrderId(order.getId());
        created.setNotificationType("ORDER_COMPLETED");
        created.setChannel("EMAIL");
        created.setRecipient(order.getCustomerEmail());
        created.setSubject(
            "Your KCMART order is complete - "
                + order.getOrderNumber()
        );
        created.setStatus("PENDING");
        created.setAttemptCount(0);
        created.setLastError(null);
        created.setSentAt(null);

        OrderNotification saved =
            notifications.save(created);

        eventPublisher.publishEvent(
            new OrderCompletionNotificationEvent(
                saved.getId()
            )
        );
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

        if (normalized.length() > 320) {
            return normalized.substring(0, 320);
        }

        return normalized;
    }

    private String normalizeNote(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        String normalized = value.trim();

        if (normalized.length() > 1000) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Status note must not exceed 1000 characters."
            );
        }

        return normalized;
    }
}