package in.kcmart.backend.order.service;

import in.kcmart.backend.order.repository.OrderInvoiceRepository;
import in.kcmart.backend.order.repository.OrderNotificationRepository;
import in.kcmart.backend.order.repository.OrderRepository;
import in.kcmart.backend.order.repository.OrderShipmentRepository;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.Mockito.*;

class OrderNotificationDeliverySafetyTest {

    private final OrderNotificationRepository notifications =
        mock(OrderNotificationRepository.class);

    private final OrderRepository orders =
        mock(OrderRepository.class);

    private final OrderInvoiceRepository invoices =
        mock(OrderInvoiceRepository.class);

    private final OrderShipmentRepository shipments =
        mock(OrderShipmentRepository.class);

    private final OrderCompletionEmailSender completionSender =
        mock(OrderCompletionEmailSender.class);

    private final OrderShipmentEmailSender shipmentSender =
        mock(OrderShipmentEmailSender.class);

    private OrderNotificationDeliveryService disabledService() {

        OrderNotificationDeliveryService service =
            new OrderNotificationDeliveryService(
                notifications,
                orders,
                invoices,
                shipments,
                completionSender,
                shipmentSender
            );

        ReflectionTestUtils.setField(
            service,
            "mailEnabled",
            false
        );

        return service;
    }

    @Test
    void disabledCompletionEmailDoesNotAccessRepositoriesOrSend() {

        disabledService().deliverCompletionEmail(999L);

        verifyNoInteractions(
            notifications,
            orders,
            invoices,
            shipments,
            completionSender,
            shipmentSender
        );
    }

    @Test
    void disabledShipmentEmailDoesNotAccessRepositoriesOrSend() {

        disabledService().deliverShipmentEmail(999L);

        verifyNoInteractions(
            notifications,
            orders,
            invoices,
            shipments,
            completionSender,
            shipmentSender
        );
    }
}