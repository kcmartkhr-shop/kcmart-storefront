package in.kcmart.backend.shipping.delhivery.client;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
    name = "delhivery.enabled",
    havingValue = "false",
    matchIfMissing = true
)
public class DisabledDelhiveryClient implements DelhiveryClient {

    @Override
    public String createShipment(String requestBody) {
        throw new IllegalStateException(
            "Delhivery integration is disabled. Shipment creation is unavailable."
        );
    }

    @Override
    public String trackShipment(String waybill) {
        throw new IllegalStateException(
            "Delhivery integration is disabled. Shipment tracking is unavailable."
        );
    }
}