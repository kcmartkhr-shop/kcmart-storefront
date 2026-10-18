package in.kcmart.backend.shipping.delhivery.service;

import in.kcmart.backend.order.entity.OrderShipment;
import in.kcmart.backend.order.repository.OrderShipmentRepository;
import in.kcmart.backend.shipping.delhivery.client.DelhiveryLabelClient;
import in.kcmart.backend.shipping.delhivery.config.DelhiveryProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DelhiveryLabelRetrievalServiceTest {

    private DelhiveryProperties properties;
    private DelhiveryLabelEligibilityService eligibility;
    private OrderShipmentRepository shipments;
    private DelhiveryLabelClient client;
    private DelhiveryLabelRetrievalService service;

    @BeforeEach
    void setup() {
        properties = mock(DelhiveryProperties.class);
        eligibility = mock(DelhiveryLabelEligibilityService.class);
        shipments = mock(OrderShipmentRepository.class);
        client = mock(DelhiveryLabelClient.class);

        service = new DelhiveryLabelRetrievalService(
            properties, eligibility, shipments, client
        );
    }

    private void eligibleOrder() {
        when(properties.isLabelRetrievalEnabled()).thenReturn(true);
        when(eligibility.check(10L)).thenReturn(
            new DelhiveryLabelEligibilityService.LabelEligibilityResult(
                true, null
            )
        );

        OrderShipment shipment = new OrderShipment();
        shipment.setOrderId(10L);
        shipment.setCarrierName("Delhivery");
        shipment.setTrackingNumber("1234567890123");

        when(shipments.findByOrderId(10L))
            .thenReturn(Optional.of(shipment));
    }

    @Test
    void disabledFlagBlocksAllDependencies() {
        assertThrows(
            IllegalStateException.class,
            () -> service.retrieve(10L)
        );

        verifyNoInteractions(eligibility, shipments, client);
    }

    @Test
    void invalidOrderIdBlocksRetrieval() {
        when(properties.isLabelRetrievalEnabled()).thenReturn(true);

        assertThrows(
            IllegalArgumentException.class,
            () -> service.retrieve(0L)
        );

        verifyNoInteractions(eligibility, shipments, client);
    }

    @Test
    void ineligibleOrderDoesNotCallClient() {
        when(properties.isLabelRetrievalEnabled()).thenReturn(true);
        when(eligibility.check(10L)).thenReturn(
            new DelhiveryLabelEligibilityService.LabelEligibilityResult(
                false, "Booking not confirmed."
            )
        );

        assertThrows(
            IllegalStateException.class,
            () -> service.retrieve(10L)
        );

        verifyNoInteractions(shipments, client);
    }

    @Test
    void missingShipmentDoesNotCallClient() {
        when(properties.isLabelRetrievalEnabled()).thenReturn(true);
        when(eligibility.check(10L)).thenReturn(
            new DelhiveryLabelEligibilityService.LabelEligibilityResult(
                true, null
            )
        );
        when(shipments.findByOrderId(10L))
            .thenReturn(Optional.empty());

        assertThrows(
            IllegalStateException.class,
            () -> service.retrieve(10L)
        );

        verifyNoInteractions(client);
    }

    @Test
    void validMockPdfIsReturnedAsDefensiveCopy() {
        eligibleOrder();

        byte[] pdf = "%PDF-1.4\nmock\n%%EOF"
            .getBytes(StandardCharsets.US_ASCII);

        when(client.fetchLabelPdf("1234567890123"))
            .thenReturn(pdf);

        byte[] result = service.retrieve(10L);

        assertArrayEquals(pdf, result);
        assertNotSame(pdf, result);
        verify(client, times(1))
            .fetchLabelPdf("1234567890123");
    }

    @Test
    void invalidPdfIsRejected() {
        eligibleOrder();

        when(client.fetchLabelPdf("1234567890123"))
            .thenReturn(
                "<html>Error</html>"
                    .getBytes(StandardCharsets.US_ASCII)
            );

        assertThrows(
            IllegalArgumentException.class,
            () -> service.retrieve(10L)
        );

        verify(client, times(1))
            .fetchLabelPdf("1234567890123");
    }
}