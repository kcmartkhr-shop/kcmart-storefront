package in.kcmart.backend.order.service;

import in.kcmart.backend.order.entity.Order;
import in.kcmart.backend.order.entity.OrderInvoice;
import in.kcmart.backend.order.entity.OrderNotification;
import in.kcmart.backend.order.repository.OrderInvoiceRepository;
import in.kcmart.backend.order.repository.OrderNotificationRepository;
import in.kcmart.backend.order.repository.OrderRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AdminInvoiceEmailRequestService {

    private static final String TYPE = "INVOICE_EMAIL";
    private static final String CHANNEL = "EMAIL";

    private final InvoiceEmailSafetyGate safetyGate;
    private final OrderRepository orders;
    private final OrderInvoiceRepository invoices;
    private final OrderNotificationRepository notifications;

    public AdminInvoiceEmailRequestService(
            InvoiceEmailSafetyGate safetyGate,
            OrderRepository orders,
            OrderInvoiceRepository invoices,
            OrderNotificationRepository notifications) {

        this.safetyGate = safetyGate;
        this.orders = orders;
        this.invoices = invoices;
        this.notifications = notifications;
    }

    @Transactional
    public OrderNotification requestInvoiceEmail(
            Long orderId,
            String requestKey) {

        safetyGate.requireEnabled();

        if (orderId == null || orderId <= 0) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Invalid order ID."
            );
        }

        if (requestKey == null
                || !requestKey.matches("[A-Za-z0-9_-]{16,100}")) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Invalid invoice email request key."
            );
        }

        Order order = orders.findByIdForUpdate(orderId)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Order not found."
            ));

        var previous = notifications
            .findByRequestKeyAndNotificationTypeAndChannel(
                requestKey,
                TYPE,
                CHANNEL
            );

        if (previous.isPresent()) {
            OrderNotification existing = previous.get();

            if (!orderId.equals(existing.getOrderId())) {
                throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Request key already belongs to another order."
                );
            }

            return existing;
        }

        if (notifications
                .existsByOrderIdAndNotificationTypeAndChannelAndStatus(
                    orderId,
                    TYPE,
                    CHANNEL,
                    "PENDING"
                )) {

            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "An invoice email request is already pending."
            );
        }

        OrderInvoice invoice = invoices.findByOrderId(orderId)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Invoice must exist before email can be requested."
            ));

        if (order.getCustomerEmail() == null
                || order.getCustomerEmail().isBlank()) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Order has no customer email."
            );
        }

        OrderNotification notification = new OrderNotification();

        notification.setOrderId(orderId);
        notification.setNotificationType(TYPE);
        notification.setChannel(CHANNEL);
        notification.setRecipient(order.getCustomerEmail());
        notification.setSubject(
            "Your KCMART Invoice - " + order.getOrderNumber()
        );
        notification.setStatus("PENDING");
        notification.setAttemptCount(0);
        notification.setRequestKey(requestKey);

        return notifications.saveAndFlush(notification);
    }
}