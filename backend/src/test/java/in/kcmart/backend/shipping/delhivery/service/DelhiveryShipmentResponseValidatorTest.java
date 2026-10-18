package in.kcmart.backend.shipping.delhivery.service;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.*;

class DelhiveryShipmentResponseValidatorTest {

    private final DelhiveryShipmentResponseValidator validator =
        new DelhiveryShipmentResponseValidator(new ObjectMapper());

    @Test
    void extractsWaybillFromAcceptedShipment() {
        String response = """
            {
              "success": true,
              "packages": [
                {
                  "status": "Success",
                  "waybill": "1234567890123"
                }
              ]
            }
            """;

        assertEquals(
            "1234567890123",
            validator.extractWaybill(response)
        );
    }

    @Test
    void rejectsUnsuccessfulResponse() {
        String response = """
            {
              "success": false,
              "packages": []
            }
            """;

        assertThrows(
            IllegalStateException.class,
            () -> validator.extractWaybill(response)
        );
    }

    @Test
    void rejectsInvalidWaybill() {
        String response = """
            {
              "success": true,
              "packages": [
                {
                  "status": "Success",
                  "waybill": "!"
                }
              ]
            }
            """;

        assertThrows(
            IllegalStateException.class,
            () -> validator.extractWaybill(response)
        );
    }

    @Test
    void rejectsMultiplePackages() {
        String response = """
            {
              "success": true,
              "packages": [
                {"status":"Success","waybill":"1234567890"},
                {"status":"Success","waybill":"9876543210"}
              ]
            }
            """;

        assertThrows(
            IllegalStateException.class,
            () -> validator.extractWaybill(response)
        );
    }

    @Test
    void rejectsMalformedJson() {
        assertThrows(
            IllegalStateException.class,
            () -> validator.extractWaybill("{invalid-json")
        );
    }

    @Test
    void rejectsEmptyResponse() {
        assertThrows(
            IllegalStateException.class,
            () -> validator.extractWaybill("")
        );
    }
}