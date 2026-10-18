package in.kcmart.backend.order.service;

import in.kcmart.backend.order.entity.Order;
import in.kcmart.backend.order.entity.OrderInvoice;
import in.kcmart.backend.order.entity.OrderNotification;
import in.kcmart.backend.order.entity.OrderShipment;
import in.kcmart.backend.order.repository.OrderInvoiceRepository;
import in.kcmart.backend.order.repository.OrderShipmentRepository;
import in.kcmart.backend.order.repository.OrderNotificationRepository;
import in.kcmart.backend.order.repository.OrderRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Service
public class OrderNotificationDeliveryService {

    @Value("${KCMART_MAIL_ENABLED:false}")
    private boolean mailEnabled;

    private final OrderNotificationRepository notifications;
    private final OrderRepository orders;
    private final OrderInvoiceRepository invoices;
    private final OrderShipmentRepository shipments;
    private final OrderCompletionEmailSender emailSender;
    private final OrderShipmentEmailSender shipmentEmailSender;

    public OrderNotificationDeliveryService(
            OrderNotificationRepository notifications,
            OrderRepository orders,
            OrderInvoiceRepository invoices,
            OrderShipmentRepository shipments,
            OrderCompletionEmailSender emailSender,
            OrderShipmentEmailSender shipmentEmailSender) {

        this.notifications = notifications;
        this.orders = orders;
        this.invoices = invoices;
        this.shipments = shipments;
        this.emailSender = emailSender;
        this.shipmentEmailSender = shipmentEmailSender;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void deliverCompletionEmail(Long notificationId) {

        if (!mailEnabled) {
            return;
        }

        OrderNotification notification = notifications
            .findByIdForUpdate(notificationId)
            .orElseThrow(() ->
                new IllegalStateException(
                    "Order notification not found: " + notificationId
                )
            );

        if ("SENT".equals(notification.getStatus())) {
            return;
        }

        int attempts = notification.getAttemptCount() == null
            ? 0
            : notification.getAttemptCount();

        notification.setAttemptCount(attempts + 1);
        notification.setLastError(null);

        try {
            if (!"ORDER_COMPLETED".equals(
                    notification.getNotificationType())) {
                throw new IllegalStateException(
                    "Unsupported notification type."
                );
            }

            if (!"EMAIL".equals(notification.getChannel())) {
                throw new IllegalStateException(
                    "Unsupported notification channel."
                );
            }

            Order order = orders
                .findById(notification.getOrderId())
                .orElseThrow(() ->
                    new IllegalStateException(
                        "Order not found for notification."
                    )
                );

            if (!"COMPLETED".equals(order.getOrderStatus())) {
                throw new IllegalStateException(
                    "Completion email requires a completed order."
                );
            }

            if (!order.getCustomerEmail().equalsIgnoreCase(
                    notification.getRecipient())) {
                throw new IllegalStateException(
                    "Notification recipient does not match order email."
                );
            }

            OrderInvoice invoice = invoices
                .findByOrderId(order.getId())
                .orElseThrow(() ->
                    new IllegalStateException(
                        "Invoice not found for completed order."
                    )
                );

            emailSender.send(order, invoice);

            notification.setStatus("SENT");
            notification.setSentAt(OffsetDateTime.now());
            notification.setLastError(null);

            notifications.save(notification);
        }
        catch (Exception ex) {

            notification.setStatus("FAILED");
            notification.setSentAt(null);
            notification.setLastError(
                safeErrorMessage(ex)
            );

            notifications.save(notification);

            // Intentionally do NOT rethrow.
            // Email failure must not affect the completed order.
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void deliverShipmentEmail(Long notificationId) {

        if (!mailEnabled) {
            return;
        }

        OrderNotification notification = notifications
            .findByIdForUpdate(notificationId)
            .orElseThrow(() ->
                new IllegalStateException(
                    "Order notification not found: " + notificationId
                )
            );

        if ("SENT".equals(notification.getStatus())) {
            return;
        }

        int attempts = notification.getAttemptCount() == null
            ? 0
            : notification.getAttemptCount();

        notification.setAttemptCount(attempts + 1);
        notification.setLastError(null);

        try {
            if (!"ORDER_SHIPPED".equals(
                    notification.getNotificationType())) {
                throw new IllegalStateException(
                    "Unsupported shipment notification type."
                );
            }

            if (!"EMAIL".equals(notification.getChannel())) {
                throw new IllegalStateException(
                    "Unsupported notification channel."
                );
            }

            Order order = orders
                .findById(notification.getOrderId())
                .orElseThrow(() ->
                    new IllegalStateException(
                        "Order not found for shipment notification."
                    )
                );

            if (!"DELIVERY".equals(order.getFulfillmentType())) {
                throw new IllegalStateException(
                    "Shipment email requires a delivery order."
                );
            }

            if (!order.getCustomerEmail().equalsIgnoreCase(
                    notification.getRecipient())) {
                throw new IllegalStateException(
                    "Notification recipient does not match order email."
                );
            }

            OrderShipment shipment = shipments
                .findByOrderId(order.getId())
                .orElseThrow(() ->
                    new IllegalStateException(
                        "Shipment not found for order."
                    )
                );

            String shipmentStatus =
                shipment.getShipmentStatus();

            if (!"SHIPPED".equals(shipmentStatus)
                    && !"OUT_FOR_DELIVERY".equals(shipmentStatus)
                    && !"DELIVERED".equals(shipmentStatus)) {
                throw new IllegalStateException(
                    "Shipment email requires a shipped order."
                );
            }

            if (shipment.getCarrierName() == null
                    || shipment.getCarrierName().isBlank()
                    || shipment.getTrackingNumber() == null
                    || shipment.getTrackingNumber().isBlank()) {
                throw new IllegalStateException(
                    "Shipment email requires carrier and tracking number."
                );
            }

            OrderInvoice invoice = invoices
                .findByOrderId(order.getId())
                .orElseThrow(() ->
                    new IllegalStateException(
                        "Shipment email requires an invoice."
                    )
                );

            shipmentEmailSender.send(
                order,
                shipment,
                invoice
            );

            notification.setStatus("SENT");
            notification.setSentAt(OffsetDateTime.now());
            notification.setLastError(null);

            notifications.save(notification);
        }
        catch (Exception ex) {

            notification.setStatus("FAILED");
            notification.setSentAt(null);
            notification.setLastError(
                safeErrorMessage(ex)
            );

            notifications.save(notification);

            // Intentionally do NOT rethrow.
            // Email failure must not affect the shipped order.
        }
    }

    private String safeErrorMessage(Exception ex) {

        String message = ex.getMessage();

        if (message == null || message.isBlank()) {
            message = ex.getClass().getSimpleName();
        }

        message = message
            .replace("\r", " ")
            .replace("\n", " ")
            .trim();

        if (message.length() > 1000) {
            message = message.substring(0, 1000);
        }

        return message;
    }
}