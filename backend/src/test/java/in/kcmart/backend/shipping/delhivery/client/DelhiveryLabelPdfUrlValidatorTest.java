package in.kcmart.backend.shipping.delhivery.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DelhiveryLabelPdfUrlValidatorTest {

    @Test
    void acceptsHttpsS3Url() {
        assertEquals(
            "s3.amazonaws.com",
            DelhiveryLabelPdfUrlValidator.validate(
                "https://s3.amazonaws.com/test-bucket/label.pdf"
            ).getHost()
        );
    }

    @Test
    void acceptsRegionalS3Url() {
        assertEquals(
            "s3.ap-south-1.amazonaws.com",
            DelhiveryLabelPdfUrlValidator.validate(
                "https://s3.ap-south-1.amazonaws.com/test/label.pdf"
            ).getHost()
        );
    }

    @Test
    void rejectsHttp() {
        assertThrows(
            IllegalArgumentException.class,
            () -> DelhiveryLabelPdfUrlValidator.validate(
                "http://s3.amazonaws.com/test/label.pdf"
            )
        );
    }

    @Test
    void rejectsLocalhost() {
        assertThrows(
            IllegalArgumentException.class,
            () -> DelhiveryLabelPdfUrlValidator.validate(
                "https://localhost/label.pdf"
            )
        );
    }

    @Test
    void rejectsLookalikeDomain() {
        assertThrows(
            IllegalArgumentException.class,
            () -> DelhiveryLabelPdfUrlValidator.validate(
                "https://s3.amazonaws.com.attacker.test/label.pdf"
            )
        );
    }

    @Test
    void rejectsCredentialsInUrl() {
        assertThrows(
            IllegalArgumentException.class,
            () -> DelhiveryLabelPdfUrlValidator.validate(
                "https://user:pass@s3.amazonaws.com/label.pdf"
            )
        );
    }

    @Test
    void rejectsCustomPort() {
        assertThrows(
            IllegalArgumentException.class,
            () -> DelhiveryLabelPdfUrlValidator.validate(
                "https://s3.amazonaws.com:8443/label.pdf"
            )
        );
    }

    @Test
    void rejectsEmptyPath() {
        assertThrows(
            IllegalArgumentException.class,
            () -> DelhiveryLabelPdfUrlValidator.validate(
                "https://s3.amazonaws.com/"
            )
        );
    }

    @Test
    void rejectsMissingUrl() {
        assertThrows(
            IllegalArgumentException.class,
            () -> DelhiveryLabelPdfUrlValidator.validate(null)
        );
    }
}