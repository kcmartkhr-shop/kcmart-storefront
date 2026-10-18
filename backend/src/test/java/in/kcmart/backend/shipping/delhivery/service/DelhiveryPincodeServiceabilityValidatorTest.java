package in.kcmart.backend.shipping.delhivery.service;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.*;

class DelhiveryPincodeServiceabilityValidatorTest {

    private final DelhiveryPincodeServiceabilityValidator validator =
        new DelhiveryPincodeServiceabilityValidator(new ObjectMapper());

    private String response(String pin, String prepaid, String remarks) {
        return """
            {
              "delivery_codes": [
                {
                  "postal_code": {
                    "pin": %s,
                    "pre_paid": "%s",
                    "remarks": "%s"
                  }
                }
              ]
            }
            """.formatted(pin, prepaid, remarks);
    }

    @Test
    void acceptsMatchingPrepaidServiceablePincode() {
        assertDoesNotThrow(() ->
            validator.requireServiceable(
                "501510", response("501510", "Y", "")
            )
        );
    }

    @Test
    void rejectsInvalidPincode() {
        assertThrows(IllegalArgumentException.class,
            () -> validator.requireServiceable("123", "{}"));
    }

    @Test
    void rejectsEmptyResponse() {
        assertThrows(IllegalStateException.class,
            () -> validator.requireServiceable("501510", ""));
    }

    @Test
    void rejectsEmptyDeliveryCodes() {
        IllegalStateException ex = assertThrows(
            IllegalStateException.class,
            () -> validator.requireServiceable(
                "501510", "{\"delivery_codes\":[]}"
            )
        );
        assertTrue(ex.getMessage().contains("NSZ"));
    }

    @Test
    void rejectsMalformedJson() {
        assertThrows(IllegalStateException.class,
            () -> validator.requireServiceable("501510", "{bad"));
    }

    @Test
    void rejectsMismatchedPincode() {
        assertThrows(IllegalStateException.class,
            () -> validator.requireServiceable(
                "501510", response("194103", "Y", "")
            )
        );
    }

    @Test
    void rejectsPrepaidDisabled() {
        assertThrows(IllegalStateException.class,
            () -> validator.requireServiceable(
                "501510", response("501510", "N", "")
            )
        );
    }

    @Test
    void rejectsEmbargo() {
        assertThrows(IllegalStateException.class,
            () -> validator.requireServiceable(
                "501510", response("501510", "Y", "Embargo")
            )
        );
    }

    @Test
    void rejectsMissingPrepaidField() {
        assertThrows(IllegalStateException.class,
            () -> validator.requireServiceable(
                "501510",
                "{\"delivery_codes\":[{\"postal_code\":{\"pin\":501510,\"remarks\":\"\"}}]}"
            )
        );
    }

    @Test
    void rejectsMissingRemarksField() {
        assertThrows(IllegalStateException.class,
            () -> validator.requireServiceable(
                "501510",
                "{\"delivery_codes\":[{\"postal_code\":{\"pin\":501510,\"pre_paid\":\"Y\"}}]}"
            )
        );
    }

    @Test
    void rejectsMissingDeliveryCodes() {
        assertThrows(IllegalStateException.class,
            () -> validator.requireServiceable("501510", "{}"));
    }

    @Test
    void rejectsLegacyUnverifiedResponse() {
        assertThrows(IllegalStateException.class,
            () -> validator.requireServiceable(
                "194103",
                "{\"delivery_codes\":[{\"postal_code\":{\"pin\":194103,\"remark\":\"\"}}]}"
            )
        );
    }
}