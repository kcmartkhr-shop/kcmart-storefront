package in.kcmart.backend.shipping.delhivery.client;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.nio.charset.StandardCharsets;

import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.entity.InputStreamEntity;
import org.apache.hc.core5.http.message.BasicClassicHttpResponse;

import static org.junit.jupiter.api.Assertions.*;

class DelhiveryApacheResponsePipelineIntegrationTest {

    private static final class TrackedResponse
            extends BasicClassicHttpResponse {

        boolean closed;

        TrackedResponse(int status) {
            super(status);
        }

        @Override
        public void close() throws IOException {
            closed = true;
            super.close();
        }
    }

    private static TrackedResponse response(
            int status,
            byte[] bytes,
            long declaredLength,
            String contentType) {

        var response = new TrackedResponse(status);

        response.setEntity(
            new InputStreamEntity(
                new ByteArrayInputStream(bytes),
                declaredLength,
                ContentType.APPLICATION_OCTET_STREAM
            )
        );

        if (contentType != null) {
            response.setHeader("Content-Type", contentType);
        }

        return response;
    }

    private static HttpRequest request(String url) {
        return HttpRequest.newBuilder(URI.create(url))
            .GET()
            .build();
    }

    private static java.net.http.HttpResponse<java.io.InputStream> bridge(
            TrackedResponse response,
            DelhiveryApacheResponseExtractor.Kind kind,
            String url) throws IOException {

        var snapshot = DelhiveryApacheResponseSnapshot.capture(
            response,
            kind
        );

        return DelhiveryJavaHttpResponseBridge.fromSnapshot(
            request(url),
            snapshot
        );
    }

    @Test
    void jsonFlowsThroughEntirePipeline() throws Exception {
        byte[] bytes = "{\"ok\":true}".getBytes(
            StandardCharsets.UTF_8
        );

        var apache = response(
            200, bytes, bytes.length, "application/json"
        );

        var result = bridge(
            apache,
            DelhiveryApacheResponseExtractor.Kind.JSON,
            "https://track.delhivery.com/api/p/packing_slip"
        );

        assertEquals(200, result.statusCode());
        assertArrayEquals(bytes, result.body().readAllBytes());
        assertEquals(
            "application/json",
            result.headers().firstValue("Content-Type").orElseThrow()
        );
        assertTrue(apache.closed);
    }

    @Test
    void pdfFlowsThroughEntirePipeline() throws Exception {
        byte[] bytes = "%PDF-1.4".getBytes(
            StandardCharsets.US_ASCII
        );

        var apache = response(
            200, bytes, bytes.length, "application/pdf"
        );

        var result = bridge(
            apache,
            DelhiveryApacheResponseExtractor.Kind.PDF,
            "https://s3.amazonaws.com/test.pdf"
        );

        assertArrayEquals(bytes, result.body().readAllBytes());
        assertTrue(apache.closed);
    }

    @Test
    void bodyIsProtectedFromMutation() throws Exception {
        var apache = response(
            200, new byte[] {1, 2, 3}, 3, "application/pdf"
        );

        var result = bridge(
            apache,
            DelhiveryApacheResponseExtractor.Kind.PDF,
            "https://s3.amazonaws.com/test.pdf"
        );

        byte[] first = result.body().readAllBytes();
        first[0] = 99;

        assertArrayEquals(
            new byte[] {1, 2, 3},
            result.body().readAllBytes()
        );
    }

    @Test
    void sensitiveHeadersAreNotForwarded() throws Exception {
        var apache = response(
            200, new byte[] {1}, 1, "application/json"
        );

        apache.setHeader("Set-Cookie", "session=secret");
        apache.setHeader("Authorization", "Token secret");

        var result = bridge(
            apache,
            DelhiveryApacheResponseExtractor.Kind.JSON,
            "https://track.delhivery.com/api/p/packing_slip"
        );

        assertTrue(
            result.headers().firstValue("Set-Cookie").isEmpty()
        );
        assertTrue(
            result.headers().firstValue("Authorization").isEmpty()
        );
        assertTrue(apache.closed);
    }

    @Test
    void oversizedResponseIsRejectedAndClosed() {
        var apache = response(
            200,
            new byte[] {1},
            DelhiveryApacheBoundedResponseReader.MAX_JSON_BYTES + 1L,
            "application/json"
        );

        assertThrows(IOException.class, () ->
            bridge(
                apache,
                DelhiveryApacheResponseExtractor.Kind.JSON,
                "https://track.delhivery.com/api/p/packing_slip"
            )
        );

        assertTrue(apache.closed);
    }

    @Test
    void httpFailureIsRejectedAndClosed() {
        var apache = response(
            500,
            new byte[] {1},
            1,
            "application/json"
        );

        assertThrows(IOException.class, () ->
            bridge(
                apache,
                DelhiveryApacheResponseExtractor.Kind.JSON,
                "https://track.delhivery.com/api/p/packing_slip"
            )
        );

        assertTrue(apache.closed);
    }

    @Test
    void bodyStreamsRemainIndependent() throws Exception {
        var apache = response(
            200, new byte[] {4, 5}, 2, "application/pdf"
        );

        var result = bridge(
            apache,
            DelhiveryApacheResponseExtractor.Kind.PDF,
            "https://s3.amazonaws.com/test.pdf"
        );

        try (var first = result.body();
             var second = result.body()) {

            assertEquals(4, first.read());
            assertEquals(4, second.read());
        }

        assertTrue(apache.closed);
    }

    @Test
    void missingEntityIsRejectedAndClosed() {
        var apache = new TrackedResponse(200);

        assertThrows(IOException.class, () ->
            bridge(
                apache,
                DelhiveryApacheResponseExtractor.Kind.JSON,
                "https://track.delhivery.com/api/p/packing_slip"
            )
        );

        assertTrue(apache.closed);
    }
}