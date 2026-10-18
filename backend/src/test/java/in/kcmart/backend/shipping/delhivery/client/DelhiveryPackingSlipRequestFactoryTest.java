package in.kcmart.backend.shipping.delhivery.client;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class DelhiveryPackingSlipRequestFactoryTest {

    private static final String PROD =
        "https://track.delhivery.com";

    private static final String STAGING =
        "https://staging-express.delhivery.com";

    @Test
    void buildsProductionRequest() {
        var request = DelhiveryPackingSlipRequestFactory.create(
            PROD, "test-token", "123456789012"
        );

        assertEquals("GET", request.method());
        assertEquals(
            PROD +
            "/api/p/packing_slip?wbns=123456789012&pdf=true&pdf_size=4R",
            request.uri().toString()
        );
        assertEquals(
            "Token test-token",
            request.headers()
                .firstValue("Authorization")
                .orElseThrow()
        );
    }

    @Test
    void buildsStagingRequest() {
        var request = DelhiveryPackingSlipRequestFactory.create(
            STAGING, "test-token", "ABC123456"
        );

        assertEquals(
            "staging-express.delhivery.com",
            request.uri().getHost()
        );
    }

    @Test
    void rejectsHttp() {
        assertThrows(
            IllegalArgumentException.class,
            () -> DelhiveryPackingSlipRequestFactory.create(
                "http://track.delhivery.com",
                "test-token",
                "123456789012"
            )
        );
    }

    @Test
    void rejectsUnknownHost() {
        assertThrows(
            IllegalArgumentException.class,
            () -> DelhiveryPackingSlipRequestFactory.create(
                "https://example.com",
                "test-token",
                "123456789012"
            )
        );
    }

    @Test
    void rejectsMissingToken() {
        assertThrows(
            IllegalArgumentException.class,
            () -> DelhiveryPackingSlipRequestFactory.create(
                PROD, "", "123456789012"
            )
        );
    }

    @Test
    void rejectsHeaderInjection() {
        assertThrows(
            IllegalArgumentException.class,
            () -> DelhiveryPackingSlipRequestFactory.create(
                PROD,
                "token\r\nInjected: value",
                "123456789012"
            )
        );
    }

    @Test
    void rejectsInvalidWaybill() {
        assertThrows(
            IllegalArgumentException.class,
            () -> DelhiveryPackingSlipRequestFactory.create(
                PROD,
                "test-token",
                "../bad"
            )
        );
    }

    @Test
    void rejectsUnapprovedBaseUrlWithPath() {
        assertThrows(
            IllegalArgumentException.class,
            () -> DelhiveryPackingSlipRequestFactory.create(
                PROD + "/extra",
                "test-token",
                "123456789012"
            )
        );
    }

    @Test
    void requestContainsNoBody() {
        var request = DelhiveryPackingSlipRequestFactory.create(
            PROD, "test-token", "123456789012"
        );

        assertTrue(request.bodyPublisher().isEmpty());
    }
}