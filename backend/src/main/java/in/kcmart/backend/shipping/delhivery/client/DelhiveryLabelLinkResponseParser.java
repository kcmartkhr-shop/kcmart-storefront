package in.kcmart.backend.shipping.delhivery.client;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import java.net.URI;

public final class DelhiveryLabelLinkResponseParser {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private DelhiveryLabelLinkResponseParser() {
    }

    public static URI parse(String json, String fieldName) {

        if (json == null || json.isBlank()) {
            throw new IllegalArgumentException(
                "Label response JSON is required."
            );
        }

        if (fieldName == null ||
                !fieldName.matches("[A-Za-z][A-Za-z0-9_]{0,63}")) {
            throw new IllegalArgumentException(
                "Explicit response field name required."
            );
        }

        try {
            JsonNode root = MAPPER.readTree(json);

            if (root == null || !root.isObject()) {
                throw new IllegalArgumentException(
                    "Label response must be a JSON object."
                );
            }

            JsonNode value = root.get(fieldName);

            if (value == null || !value.isTextual()) {
                throw new IllegalArgumentException(
                    "Configured PDF link field missing."
                );
            }

            return DelhiveryLabelPdfUrlValidator.validate(
                value.asText()
            );

        } catch (tools.jackson.core.JacksonException ex) {
            throw new IllegalArgumentException(
                "Invalid label response JSON.", ex
            );
        }
    }
}