package in.kcmart.backend.order.service;

import in.kcmart.backend.order.repository.OrderInvoiceRepository;
import in.kcmart.backend.order.repository.OrderNotificationRepository;
import in.kcmart.backend.order.repository.OrderRepository;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class AdminInvoiceEmailRequestServiceTest {

    private final OrderRepository orders = mock(OrderRepository.class);
    private final OrderInvoiceRepository invoices = mock(OrderInvoiceRepository.class);
    private final OrderNotificationRepository notifications =
        mock(OrderNotificationRepository.class);

    @Test
    void disabledEmailRejectsBeforeDatabaseAccess() {

        AdminInvoiceEmailRequestService service =
            new AdminInvoiceEmailRequestService(
                new InvoiceEmailSafetyGate(false),
                orders,
                invoices,
                notifications
            );

        ResponseStatusException exception = assertThrows(
            ResponseStatusException.class,
            () -> service.requestInvoiceEmail(
                123L,
                "invoice-request-123456"
            )
        );

        assertEquals(503, exception.getStatusCode().value());

        verifyNoInteractions(orders, invoices, notifications);
    }

    @Test
    void invalidRequestKeyRejectsBeforeDatabaseAccess() {

        AdminInvoiceEmailRequestService service =
            new AdminInvoiceEmailRequestService(
                new InvoiceEmailSafetyGate(true),
                orders,
                invoices,
                notifications
            );

        ResponseStatusException exception = assertThrows(
            ResponseStatusException.class,
            () -> service.requestInvoiceEmail(123L, "short")
        );

        assertEquals(400, exception.getStatusCode().value());

        verifyNoInteractions(orders, invoices, notifications);
    }
}