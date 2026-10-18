package in.kcmart.backend.shipping.delhivery.client;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.io.entity.InputStreamEntity;
import org.apache.hc.core5.http.message.BasicClassicHttpResponse;
import org.apache.hc.core5.http.ContentType;

import static org.junit.jupiter.api.Assertions.*;

class DelhiveryApacheResponseExtractorTest {

    private static final class TrackingResponse
            extends BasicClassicHttpResponse {

        boolean closed;

        TrackingResponse(int status) {
            super(status);
        }

        @Override
        public void close() throws IOException {
            closed = true;
            super.close();
        }
    }

    private static TrackingResponse response(
            int status,
            byte[] bytes,
            long declaredLength) {

        TrackingResponse response = new TrackingResponse(status);

        response.setEntity(
            new InputStreamEntity(
                new ByteArrayInputStream(bytes),
                declaredLength,
                ContentType.APPLICATION_OCTET_STREAM
            )
        );

        return response;
    }

    @Test
    void extractsJsonAndClosesResponse() throws Exception {
        byte[] bytes = "{}".getBytes(StandardCharsets.UTF_8);
        var response = response(200, bytes, bytes.length);

        assertArrayEquals(
            bytes,
            DelhiveryApacheResponseExtractor.extract(
                response,
                DelhiveryApacheResponseExtractor.Kind.JSON
            )
        );

        assertTrue(response.closed);
    }

    @Test
    void extractsPdfAndClosesResponse() throws Exception {
        byte[] bytes = "%PDF-1.4".getBytes(StandardCharsets.US_ASCII);
        var response = response(200, bytes, bytes.length);

        assertArrayEquals(
            bytes,
            DelhiveryApacheResponseExtractor.extract(
                response,
                DelhiveryApacheResponseExtractor.Kind.PDF
            )
        );

        assertTrue(response.closed);
    }

    @Test
    void rejectsHttpErrorAndClosesResponse() {
        var response = response(500, new byte[] {1}, 1);

        assertThrows(IOException.class, () ->
            DelhiveryApacheResponseExtractor.extract(
                response,
                DelhiveryApacheResponseExtractor.Kind.JSON
            )
        );

        assertTrue(response.closed);
    }

    @Test
    void rejectsOversizedJsonAndClosesResponse() {
        var response = response(
            200,
            new byte[] {1},
            DelhiveryApacheBoundedResponseReader.MAX_JSON_BYTES + 1L
        );

        assertThrows(IOException.class, () ->
            DelhiveryApacheResponseExtractor.extract(
                response,
                DelhiveryApacheResponseExtractor.Kind.JSON
            )
        );

        assertTrue(response.closed);
    }

    @Test
    void rejectsOversizedPdfAndClosesResponse() {
        var response = response(
            200,
            new byte[] {1},
            DelhiveryApacheBoundedResponseReader.MAX_PDF_BYTES + 1L
        );

        assertThrows(IOException.class, () ->
            DelhiveryApacheResponseExtractor.extract(
                response,
                DelhiveryApacheResponseExtractor.Kind.PDF
            )
        );

        assertTrue(response.closed);
    }

    @Test
    void rejectsMissingEntityAndClosesResponse() {
        TrackingResponse response = new TrackingResponse(200);

        assertThrows(IOException.class, () ->
            DelhiveryApacheResponseExtractor.extract(
                response,
                DelhiveryApacheResponseExtractor.Kind.JSON
            )
        );

        assertTrue(response.closed);
    }

    @Test
    void rejectsEmptyEntityAndClosesResponse() {
        var response = response(200, new byte[0], 0);

        assertThrows(IOException.class, () ->
            DelhiveryApacheResponseExtractor.extract(
                response,
                DelhiveryApacheResponseExtractor.Kind.JSON
            )
        );

        assertTrue(response.closed);
    }

    @Test
    void rejectsLengthMismatchAndClosesResponse() {
        var response = response(200, new byte[] {1, 2}, 10);

        assertThrows(IOException.class, () ->
            DelhiveryApacheResponseExtractor.extract(
                response,
                DelhiveryApacheResponseExtractor.Kind.JSON
            )
        );

        assertTrue(response.closed);
    }

    @Test
    void rejectsInvalidNegativeLengthAndClosesResponse() {
        var response = response(200, new byte[] {1}, -2);

        assertThrows(IOException.class, () ->
            DelhiveryApacheResponseExtractor.extract(
                response,
                DelhiveryApacheResponseExtractor.Kind.JSON
            )
        );

        assertTrue(response.closed);
    }

    @Test
    void acceptsUnknownContentLength() throws Exception {
        var response = response(200, new byte[] {1}, -1);

        assertArrayEquals(
            new byte[] {1},
            DelhiveryApacheResponseExtractor.extract(
                response,
                DelhiveryApacheResponseExtractor.Kind.JSON
            )
        );

        assertTrue(response.closed);
    }
}