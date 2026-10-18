package in.kcmart.backend.shipping.delhivery.client;

import org.junit.jupiter.api.Test;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import static org.junit.jupiter.api.Assertions.*;

class DelhiveryLabelPdfResponseReaderTest {

    private byte[] pdf() {
        return "%PDF-1.4\n1 0 obj\nendobj\n%%EOF\n"
            .getBytes(StandardCharsets.US_ASCII);
    }

    @Test
    void acceptsValidPdf() throws IOException {
        byte[] result = DelhiveryLabelPdfResponseReader.read(
            new ByteArrayInputStream(pdf())
        );

        assertArrayEquals(pdf(), result);
    }

    @Test
    void rejectsNullStream() {
        assertThrows(
            IllegalArgumentException.class,
            () -> DelhiveryLabelPdfResponseReader.read(null)
        );
    }

    @Test
    void rejectsEmptyStream() {
        assertThrows(
            IllegalArgumentException.class,
            () -> DelhiveryLabelPdfResponseReader.read(
                new ByteArrayInputStream(new byte[0])
            )
        );
    }

    @Test
    void rejectsNonPdf() {
        assertThrows(
            IllegalArgumentException.class,
            () -> DelhiveryLabelPdfResponseReader.read(
                new ByteArrayInputStream(
                    "not a pdf".getBytes(StandardCharsets.US_ASCII)
                )
            )
        );
    }

    @Test
    void rejectsOversizedPdf() {
        byte[] oversized = new byte[5 * 1024 * 1024 + 1];

        byte[] header = "%PDF-1.4".getBytes(
            StandardCharsets.US_ASCII
        );

        System.arraycopy(
            header, 0, oversized, 0, header.length
        );

        assertThrows(
            IllegalArgumentException.class,
            () -> DelhiveryLabelPdfResponseReader.read(
                new ByteArrayInputStream(oversized)
            )
        );
    }

    @Test
    void rejectsMissingEof() {
        assertThrows(
            IllegalArgumentException.class,
            () -> DelhiveryLabelPdfResponseReader.read(
                new ByteArrayInputStream(
                    "%PDF-1.4\nbody".getBytes(
                        StandardCharsets.US_ASCII
                    )
                )
            )
        );
    }
}