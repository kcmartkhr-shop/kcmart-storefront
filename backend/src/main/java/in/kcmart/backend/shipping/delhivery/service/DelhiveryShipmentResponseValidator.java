package in.kcmart.backend.shipping.delhivery.service;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

@Component
public class DelhiveryShipmentResponseValidator {

    private final ObjectMapper mapper;

    public DelhiveryShipmentResponseValidator(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    public String extractWaybill(String responseBody) {

        if (responseBody == null || responseBody.isBlank()) {
            throw new IllegalStateException(
                "Delhivery returned an empty shipment response."
            );
        }

        try {
            JsonNode root = mapper.readTree(responseBody);

            if (!root.isObject()) {
                throw new IllegalStateException(
                    "Invalid Delhivery shipment response."
                );
            }

            if (!root.path("success").asBoolean(false)) {
                throw new IllegalStateException(
                    "Delhivery did not confirm shipment creation."
                );
            }

            JsonNode packages = root.path("packages");

            if (!packages.isArray() || packages.size() != 1) {
                throw new IllegalStateException(
                    "Expected exactly one Delhivery shipment package."
                );
            }

            JsonNode pkg = packages.get(0);

            if (!pkg.isObject()) {
                throw new IllegalStateException(
                    "Invalid Delhivery package response."
                );
            }

            String status = pkg.path("status").asText("");

            if (!"Success".equalsIgnoreCase(status)) {
                throw new IllegalStateException(
                    "Delhivery package was not accepted."
                );
            }

            String waybill = pkg.path("waybill").asText("");

            if (!waybill.matches("[A-Za-z0-9-]{5,50}")) {
                throw new IllegalStateException(
                    "Delhivery returned an invalid waybill."
                );
            }

            return waybill;

        } catch (tools.jackson.core.JacksonException ex) {
            throw new IllegalStateException(
                "Delhivery returned invalid JSON.",
                ex
            );
        }
    }
}