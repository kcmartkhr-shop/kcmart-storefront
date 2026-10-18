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

class DelhiveryLabelRetrievalSecurityTest {

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

        when(properties.isLabelRetrievalEnabled()).thenReturn(true);
        when(eligibility.check(10L)).thenReturn(
            new DelhiveryLabelEligibilityService.LabelEligibilityResult(
                true, null
            )
        );
    }

    private void shipment(
            Long orderId,
            String carrier,
            String awb) {

        OrderShipment shipment = new OrderShipment();
        shipment.setOrderId(orderId);
        shipment.setCarrierName(carrier);
        shipment.setTrackingNumber(awb);

        when(shipments.findByOrderId(10L))
            .thenReturn(Optional.of(shipment));
    }

    @Test
    void rejectsShipmentBelongingToAnotherOrder() {
        shipment(20L, "Delhivery", "1234567890123");

        assertThrows(
            IllegalStateException.class,
            () -> service.retrieve(10L)
        );

        verifyNoInteractions(client);
    }

    @Test
    void rejectsWrongCarrier() {
        shipment(10L, "OtherCarrier", "1234567890123");

        assertThrows(
            IllegalStateException.class,
            () -> service.retrieve(10L)
        );

        verifyNoInteractions(client);
    }

    @Test
    void rejectsMissingAwb() {
        shipment(10L, "Delhivery", null);

        assertThrows(
            IllegalStateException.class,
            () -> service.retrieve(10L)
        );

        verifyNoInteractions(client);
    }

    @Test
    void rejectsUnsafeAwb() {
        shipment(10L, "Delhivery", "../unsafe");

        assertThrows(
            IllegalStateException.class,
            () -> service.retrieve(10L)
        );

        verifyNoInteractions(client);
    }

    @Test
    void rejectsNullEligibilityResult() {
        when(eligibility.check(10L)).thenReturn(null);

        assertThrows(
            IllegalStateException.class,
            () -> service.retrieve(10L)
        );

        verifyNoInteractions(shipments, client);
    }

    @Test
    void rejectsMissingPdfEof() {
        shipment(10L, "Delhivery", "1234567890123");

        when(client.fetchLabelPdf("1234567890123"))
            .thenReturn(
                "%PDF-1.4\nincomplete"
                    .getBytes(StandardCharsets.US_ASCII)
            );

        assertThrows(
            IllegalArgumentException.class,
            () -> service.retrieve(10L)
        );
    }
}