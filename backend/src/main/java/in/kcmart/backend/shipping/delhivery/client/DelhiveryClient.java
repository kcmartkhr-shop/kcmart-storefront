package in.kcmart.backend.shipping.delhivery.client;

public interface DelhiveryClient {

    String createShipment(String requestBody);

    String trackShipment(String waybill);

}