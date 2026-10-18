package in.kcmart.backend.shipping.delhivery.service;

import in.kcmart.backend.shipping.delhivery.config.DelhiveryProperties;
import org.springframework.stereotype.Service;

@Service
public class DelhiveryBookingPreflightService {

    private final DelhiveryProperties properties;
    private final DelhiveryServiceabilityGate serviceabilityGate;

    public DelhiveryBookingPreflightService(
            DelhiveryProperties properties,
            DelhiveryServiceabilityGate serviceabilityGate) {

        this.properties = properties;
        this.serviceabilityGate = serviceabilityGate;
    }

    public void verifyServiceability(String destinationPincode) {

        if (!properties.isShipmentCreationEnabled()) {
            throw new IllegalStateException(
                "Delhivery shipment creation is disabled."
            );
        }

        if (destinationPincode == null ||
                !destinationPincode.matches("[1-9][0-9]{5}")) {
            throw new IllegalArgumentException(
                "Invalid destination pincode."
            );
        }

        serviceabilityGate.requireServiceable(destinationPincode);
    }
}