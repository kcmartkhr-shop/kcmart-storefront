package in.kcmart.backend.shipping.delhivery.service;

import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Component
public class DelhiveryPincodeServiceabilityValidator {

    private final ObjectMapper mapper;

    public DelhiveryPincodeServiceabilityValidator(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    public void requireServiceable(String requestedPincode, String responseBody) {

        if (requestedPincode == null ||
                !requestedPincode.matches("[1-9][0-9]{5}")) {
            throw new IllegalArgumentException("Invalid destination pincode.");
        }

        if (responseBody == null || responseBody.isBlank()) {
            throw new IllegalStateException(
                "Empty Delhivery serviceability response."
            );
        }

        final JsonNode root;

        try {
            root = mapper.readTree(responseBody);
        } catch (Exception ex) {
            throw new IllegalStateException(
                "Invalid Delhivery serviceability JSON.", ex
            );
        }

        if (root == null || !root.isObject()) {
            throw new IllegalStateException(
                "Invalid Delhivery serviceability response structure."
            );
        }

        JsonNode codes = root.path("delivery_codes");

        if (!codes.isArray()) {
            throw new IllegalStateException(
                "Missing Delhivery delivery_codes array."
            );
        }

        if (codes.isEmpty()) {
            throw new IllegalStateException(
                "Destination pincode is not serviceable (NSZ)."
            );
        }

        for (JsonNode entry : codes) {

            JsonNode postal = entry.path("postal_code");

            if (!postal.isObject()) {
                continue;
            }

            JsonNode pin = postal.path("pin");

            if (!pin.isIntegralNumber() &&
                    !pin.isTextual()) {
                continue;
            }

            if (!requestedPincode.equals(pin.asText())) {
                continue;
            }

            String prepaid = postal.path("pre_paid").asText("");
            String remarks = postal.path("remarks").asText("__MISSING__");

            if (!"Y".equalsIgnoreCase(prepaid)) {
                throw new IllegalStateException(
                    "Prepaid delivery is not supported for this pincode."
                );
            }

            if (!remarks.isBlank()) {
                throw new IllegalStateException(
                    "Delhivery pincode has a serviceability restriction."
                );
            }

            return;
        }

        throw new IllegalStateException(
            "Delhivery response does not match the requested pincode."
        );
    }
}