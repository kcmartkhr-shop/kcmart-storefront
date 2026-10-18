package in.kcmart.backend.shipping.delhivery.service;

import in.kcmart.backend.shipping.delhivery.client.DelhiveryClient;
import org.springframework.stereotype.Service;

@Service
public class DelhiveryTrackingService {

    private final DelhiveryClient client;

    public DelhiveryTrackingService(DelhiveryClient client) {
        this.client = client;
    }

    public String track(String waybill) {

        if (waybill == null
                || !waybill.matches("[A-Za-z0-9-]{5,50}")) {
            throw new IllegalArgumentException(
                "Invalid Delhivery tracking number."
            );
        }

        return client.trackShipment(waybill);
    }
}