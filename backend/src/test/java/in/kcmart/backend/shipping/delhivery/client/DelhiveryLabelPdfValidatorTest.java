package in.kcmart.backend.shipping.delhivery.client;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class DelhiveryLabelPdfValidatorTest {

    private byte[] bytes(String text) {
        return text.getBytes(StandardCharsets.US_ASCII);
    }

    @Test
    void acceptsBasicPdfStructure() {
        byte[] input = bytes("%PDF-1.4\nsample\n%%EOF");

        byte[] result = DelhiveryLabelPdfValidator.validate(input);

        assertArrayEquals(input, result);
        assertNotSame(input, result);
    }

    @Test
    void rejectsNullResponse() {
        assertThrows(
            IllegalArgumentException.class,
            () -> DelhiveryLabelPdfValidator.validate(null)
        );
    }

    @Test
    void rejectsEmptyResponse() {
        assertThrows(
            IllegalArgumentException.class,
            () -> DelhiveryLabelPdfValidator.validate(new byte[0])
        );
    }

    @Test
    void rejectsHtmlResponse() {
        assertThrows(
            IllegalArgumentException.class,
            () -> DelhiveryLabelPdfValidator.validate(
                bytes("<html>Error</html>")
            )
        );
    }

    @Test
    void rejectsJsonResponse() {
        assertThrows(
            IllegalArgumentException.class,
            () -> DelhiveryLabelPdfValidator.validate(
                bytes("{\"error\":\"invalid token\"}")
            )
        );
    }

    @Test
    void rejectsMissingPdfHeader() {
        assertThrows(
            IllegalArgumentException.class,
            () -> DelhiveryLabelPdfValidator.validate(
                bytes("NOTPDF\n%%EOF")
            )
        );
    }

    @Test
    void rejectsMissingEofMarker() {
        assertThrows(
            IllegalArgumentException.class,
            () -> DelhiveryLabelPdfValidator.validate(
                bytes("%PDF-1.4\nincomplete")
            )
        );
    }

    @Test
    void rejectsOversizedResponse() {
        byte[] oversized = new byte[
            DelhiveryLabelPdfValidator.MAX_PDF_BYTES + 1
        ];

        assertThrows(
            IllegalArgumentException.class,
            () -> DelhiveryLabelPdfValidator.validate(oversized)
        );
    }

    @Test
    void acceptsTrailingWhitespaceAfterEof() {
        byte[] input = bytes("%PDF-1.4\nsample\n%%EOF\n");

        assertArrayEquals(
            input,
            DelhiveryLabelPdfValidator.validate(input)
        );
    }
}