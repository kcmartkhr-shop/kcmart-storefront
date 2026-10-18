package in.kcmart.backend.shipping.delhivery.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class DelhiveryProperties {

    private final boolean enabled;
    private final boolean shipmentCreationEnabled;
    private final boolean labelRetrievalEnabled;
    private final String baseUrl;
    private final String apiToken;
    private final String pickupLocation;

    @org.springframework.beans.factory.annotation.Autowired
    public DelhiveryProperties(
            @Value("${delhivery.enabled:false}") boolean enabled,
            @Value("${delhivery.shipment-creation-enabled:false}") boolean shipmentCreationEnabled,
            @Value("${delhivery.label-retrieval-enabled:false}") boolean labelRetrievalEnabled,
            @Value("${delhivery.base-url:https://track.delhivery.com}") String baseUrl,
            @Value("${delhivery.api-token:}") String apiToken,
            @Value("${delhivery.pickup-location:}") String pickupLocation) {

        this.enabled = enabled;
        this.shipmentCreationEnabled = shipmentCreationEnabled;
        this.labelRetrievalEnabled = labelRetrievalEnabled;
        this.baseUrl = baseUrl;
        this.apiToken = apiToken;
        this.pickupLocation = pickupLocation;
    }

    public DelhiveryProperties(
            boolean enabled,
            boolean shipmentCreationEnabled,
            String baseUrl,
            String apiToken,
            String pickupLocation) {
        this(
            enabled,
            shipmentCreationEnabled,
            false,
            baseUrl,
            apiToken,
            pickupLocation
        );
    }
    public DelhiveryProperties(
            boolean enabled,
            String baseUrl,
            String apiToken,
            String pickupLocation) {
        this(enabled, false, false, baseUrl, apiToken, pickupLocation);
    }
    public boolean isLabelRetrievalEnabled() {
        return enabled && labelRetrievalEnabled;
    }

    public boolean isShipmentCreationEnabled() {
        return enabled && shipmentCreationEnabled;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public String getApiToken() {
        return apiToken;
    }

    public String getPickupLocation() {
        return pickupLocation;
    }
}