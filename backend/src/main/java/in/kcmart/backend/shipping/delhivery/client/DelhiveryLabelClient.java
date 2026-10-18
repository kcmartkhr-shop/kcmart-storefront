package in.kcmart.backend.shipping.delhivery.client;

/**
 * Read-only shipping label retrieval contract.
 * Implementations must not create shipments or schedule pickups.
 */
public interface DelhiveryLabelClient {

    byte[] fetchLabelPdf(String waybill);
}