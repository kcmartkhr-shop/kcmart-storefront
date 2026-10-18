package in.kcmart.backend.shipping.delhivery.client;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class DelhiveryLabelLinkResponseParserTest {

    private static final String URL =
        "https://s3.amazonaws.com/test-bucket/label.pdf";

    @Test
    void acceptsExplicitField() {
        var result = DelhiveryLabelLinkResponseParser.parse(
            "{\"sample_link\":\"" + URL + "\"}",
            "sample_link"
        );

        assertEquals(URL, result.toString());
    }

    @Test
    void rejectsUnknownField() {
        assertThrows(
            IllegalArgumentException.class,
            () -> DelhiveryLabelLinkResponseParser.parse(
                "{\"sample_link\":\"" + URL + "\"}",
                "different_field"
            )
        );
    }

    @Test
    void rejectsMissingJson() {
        assertThrows(
            IllegalArgumentException.class,
            () -> DelhiveryLabelLinkResponseParser.parse(
                null, "sample_link"
            )
        );
    }

    @Test
    void rejectsMalformedJson() {
        assertThrows(
            IllegalArgumentException.class,
            () -> DelhiveryLabelLinkResponseParser.parse(
                "{invalid", "sample_link"
            )
        );
    }

    @Test
    void rejectsArrayResponse() {
        assertThrows(
            IllegalArgumentException.class,
            () -> DelhiveryLabelLinkResponseParser.parse(
                "[]", "sample_link"
            )
        );
    }

    @Test
    void rejectsNonTextLink() {
        assertThrows(
            IllegalArgumentException.class,
            () -> DelhiveryLabelLinkResponseParser.parse(
                "{\"sample_link\":123}", "sample_link"
            )
        );
    }

    @Test
    void rejectsUnsafeLink() {
        assertThrows(
            IllegalArgumentException.class,
            () -> DelhiveryLabelLinkResponseParser.parse(
                "{\"sample_link\":\"http://localhost/test.pdf\"}",
                "sample_link"
            )
        );
    }

    @Test
    void rejectsInvalidFieldName() {
        assertThrows(
            IllegalArgumentException.class,
            () -> DelhiveryLabelLinkResponseParser.parse(
                "{\"sample_link\":\"" + URL + "\"}",
                "../sample_link"
            )
        );
    }
}