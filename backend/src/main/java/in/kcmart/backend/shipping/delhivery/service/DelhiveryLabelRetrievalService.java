package in.kcmart.backend.shipping.delhivery.service;

import in.kcmart.backend.order.entity.OrderShipment;
import in.kcmart.backend.order.repository.OrderShipmentRepository;
import in.kcmart.backend.shipping.delhivery.client.DelhiveryLabelClient;
import in.kcmart.backend.shipping.delhivery.client.DelhiveryLabelPdfValidator;
import in.kcmart.backend.shipping.delhivery.config.DelhiveryProperties;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DelhiveryLabelRetrievalService {

    private final DelhiveryProperties properties;
    private final DelhiveryLabelEligibilityService eligibility;
    private final OrderShipmentRepository shipments;
    private final DelhiveryLabelClient client;

    public DelhiveryLabelRetrievalService(
            DelhiveryProperties properties,
            DelhiveryLabelEligibilityService eligibility,
            OrderShipmentRepository shipments,
            DelhiveryLabelClient client) {

        this.properties = properties;
        this.eligibility = eligibility;
        this.shipments = shipments;
        this.client = client;
    }

    @Transactional(readOnly = true)
    public byte[] retrieve(Long orderId) {

        if (!properties.isLabelRetrievalEnabled()) {
            throw new IllegalStateException(
                "Delhivery label retrieval is disabled."
            );
        }

        if (orderId == null || orderId <= 0) {
            throw new IllegalArgumentException(
                "Valid order ID required."
            );
        }

        var result = eligibility.check(orderId);

        if (result == null || !result.eligible()) {
            throw new IllegalStateException(
                "Delhivery label eligibility not confirmed."
            );
        }

        OrderShipment shipment = shipments.findByOrderId(orderId)
            .orElseThrow(() -> new IllegalStateException(
                "Order shipment not found."
            ));

        if (!orderId.equals(shipment.getOrderId())
                || !"Delhivery".equalsIgnoreCase(
                    shipment.getCarrierName())) {
            throw new IllegalStateException(
                "Delhivery shipment identity mismatch."
            );
        }

        String waybill = shipment.getTrackingNumber();

        if (waybill == null ||
                !waybill.matches("[A-Za-z0-9-]{5,50}")) {
            throw new IllegalStateException(
                "Confirmed Delhivery AWB required."
            );
        }

        byte[] response = client.fetchLabelPdf(waybill);

        return DelhiveryLabelPdfValidator.validate(response);
    }
}