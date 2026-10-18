package in.kcmart.backend.shipping.delhivery.service;

import in.kcmart.backend.shipping.delhivery.client.DelhiveryServiceabilityClient;
import org.springframework.stereotype.Service;

@Service
public class DelhiveryServiceabilityGate {

    private final DelhiveryServiceabilityClient client;
    private final DelhiveryPincodeServiceabilityValidator validator;

    public DelhiveryServiceabilityGate(
            DelhiveryServiceabilityClient client,
            DelhiveryPincodeServiceabilityValidator validator) {
        this.client = client;
        this.validator = validator;
    }

    public void requireServiceable(String pincode) {
        String response = client.fetchPincode(pincode);
        validator.requireServiceable(pincode, response);
    }
}