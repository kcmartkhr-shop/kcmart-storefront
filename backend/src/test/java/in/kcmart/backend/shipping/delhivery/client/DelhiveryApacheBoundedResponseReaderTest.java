package in.kcmart.backend.shipping.delhivery.client;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class DelhiveryApacheBoundedResponseReaderTest {

    private static InputStream stream(byte[] bytes) {
        return new ByteArrayInputStream(bytes);
    }

    @Test
    void acceptsSmallJson() throws Exception {
        byte[] bytes = "{}".getBytes(StandardCharsets.UTF_8);

        assertArrayEquals(
            bytes,
            DelhiveryApacheBoundedResponseReader.readJson(
                200, bytes.length, stream(bytes)
            )
        );
    }

    @Test
    void acceptsSmallPdfBytes() throws Exception {
        byte[] bytes = "%PDF-1.4".getBytes(StandardCharsets.US_ASCII);

        assertArrayEquals(
            bytes,
            DelhiveryApacheBoundedResponseReader.readPdf(
                200, bytes.length, stream(bytes)
            )
        );
    }

    @Test
    void rejectsNonSuccessStatus() {
        assertThrows(IOException.class, () ->
            DelhiveryApacheBoundedResponseReader.readJson(
                302, 2, stream(new byte[] {1, 2})
            )
        );
    }

    @Test
    void rejectsOversizedDeclaredJson() {
        assertThrows(IOException.class, () ->
            DelhiveryApacheBoundedResponseReader.readJson(
                200,
                DelhiveryApacheBoundedResponseReader.MAX_JSON_BYTES + 1L,
                stream(new byte[] {1})
            )
        );
    }

    @Test
    void rejectsOversizedActualJson() {
        byte[] bytes = new byte[
            DelhiveryApacheBoundedResponseReader.MAX_JSON_BYTES + 1
        ];

        assertThrows(IOException.class, () ->
            DelhiveryApacheBoundedResponseReader.readJson(
                200, -1, stream(bytes)
            )
        );
    }

    @Test
    void rejectsOversizedActualPdf() {
        byte[] bytes = new byte[
            DelhiveryApacheBoundedResponseReader.MAX_PDF_BYTES + 1
        ];

        assertThrows(IOException.class, () ->
            DelhiveryApacheBoundedResponseReader.readPdf(
                200, -1, stream(bytes)
            )
        );
    }

    @Test
    void rejectsEmptyResponse() {
        assertThrows(IOException.class, () ->
            DelhiveryApacheBoundedResponseReader.readJson(
                200, 0, stream(new byte[0])
            )
        );
    }

    @Test
    void rejectsContentLengthMismatch() {
        assertThrows(IOException.class, () ->
            DelhiveryApacheBoundedResponseReader.readJson(
                200, 10, stream(new byte[] {1, 2})
            )
        );
    }

    @Test
    void acceptsUnknownContentLength() throws Exception {
        byte[] bytes = new byte[] {1, 2, 3};

        assertArrayEquals(
            bytes,
            DelhiveryApacheBoundedResponseReader.readJson(
                200, -1, stream(bytes)
            )
        );
    }

    @Test
    void closesStreamAfterSuccess() throws Exception {
        TrackingStream input = new TrackingStream(new byte[] {1});

        DelhiveryApacheBoundedResponseReader.readJson(
            200, 1, input
        );

        assertTrue(input.closed);
    }

    @Test
    void closesStreamAfterRejection() {
        TrackingStream input = new TrackingStream(new byte[] {1});

        assertThrows(IOException.class, () ->
            DelhiveryApacheBoundedResponseReader.readJson(
                500, 1, input
            )
        );

        assertTrue(input.closed);
    }

    @Test
    void rejectsNegativeContentLengthBelowUnknownMarker() {
        assertThrows(IOException.class, () ->
            DelhiveryApacheBoundedResponseReader.readJson(
                200, -2, stream(new byte[] {1})
            )
        );
    }

    private static final class TrackingStream
            extends ByteArrayInputStream {

        boolean closed;

        TrackingStream(byte[] bytes) {
            super(bytes);
        }

        @Override
        public void close() throws IOException {
            closed = true;
            super.close();
        }
    }
}