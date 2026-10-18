package in.kcmart.backend.shipping.delhivery.client;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.entity.ByteArrayEntity;
import org.apache.hc.core5.http.message.BasicClassicHttpResponse;

import static org.junit.jupiter.api.Assertions.*;

class DelhiveryApacheResponseSnapshotTest {

    private static BasicClassicHttpResponse response(
            int status,
            byte[] bytes,
            String contentType) {

        var response = new BasicClassicHttpResponse(status);

        response.setEntity(
            new ByteArrayEntity(
                bytes,
                ContentType.APPLICATION_OCTET_STREAM
            )
        );

        if (contentType != null) {
            response.setHeader("Content-Type", contentType);
        }

        return response;
    }

    @Test
    void capturesStatus() throws Exception {
        var snapshot = DelhiveryApacheResponseSnapshot.capture(
            response(200, new byte[] {1}, "application/pdf"),
            DelhiveryApacheResponseExtractor.Kind.PDF
        );

        assertEquals(200, snapshot.statusCode());
    }

    @Test
    void capturesBodyBytes() throws Exception {
        byte[] bytes = "{}".getBytes(StandardCharsets.UTF_8);

        var snapshot = DelhiveryApacheResponseSnapshot.capture(
            response(200, bytes, "application/json"),
            DelhiveryApacheResponseExtractor.Kind.JSON
        );

        assertArrayEquals(bytes, snapshot.bodyBytes());
    }

    @Test
    void providesReadableBodyStream() throws Exception {
        var snapshot = DelhiveryApacheResponseSnapshot.capture(
            response(200, new byte[] {1, 2, 3}, "application/pdf"),
            DelhiveryApacheResponseExtractor.Kind.PDF
        );

        try (var input = snapshot.openBodyStream()) {
            assertArrayEquals(
                new byte[] {1, 2, 3},
                input.readAllBytes()
            );
        }
    }

    @Test
    void protectsBodyFromExternalMutation() throws Exception {
        var snapshot = DelhiveryApacheResponseSnapshot.capture(
            response(200, new byte[] {1, 2}, "application/pdf"),
            DelhiveryApacheResponseExtractor.Kind.PDF
        );

        byte[] copy = snapshot.bodyBytes();
        copy[0] = 99;

        assertArrayEquals(
            new byte[] {1, 2},
            snapshot.bodyBytes()
        );
    }

    @Test
    void capturesSafeContentType() throws Exception {
        var snapshot = DelhiveryApacheResponseSnapshot.capture(
            response(200, new byte[] {1}, "application/pdf"),
            DelhiveryApacheResponseExtractor.Kind.PDF
        );

        assertEquals(
            "application/pdf",
            snapshot.safeHeaders().get("content-type")
        );
    }

    @Test
    void excludesSensitiveHeaders() throws Exception {
        var response = response(
            200,
            new byte[] {1},
            "application/json"
        );

        response.setHeader("Set-Cookie", "session=secret");
        response.setHeader("Authorization", "Token secret");

        var snapshot = DelhiveryApacheResponseSnapshot.capture(
            response,
            DelhiveryApacheResponseExtractor.Kind.JSON
        );

        assertFalse(snapshot.safeHeaders().containsKey("set-cookie"));
        assertFalse(snapshot.safeHeaders().containsKey("authorization"));
    }

    @Test
    void returnsImmutableHeaders() throws Exception {
        var snapshot = DelhiveryApacheResponseSnapshot.capture(
            response(200, new byte[] {1}, "application/pdf"),
            DelhiveryApacheResponseExtractor.Kind.PDF
        );

        assertThrows(
            UnsupportedOperationException.class,
            () -> snapshot.safeHeaders().put("x-test", "value")
        );
    }

    @Test
    void reportsBodySize() throws Exception {
        var snapshot = DelhiveryApacheResponseSnapshot.capture(
            response(200, new byte[] {1, 2, 3}, "application/pdf"),
            DelhiveryApacheResponseExtractor.Kind.PDF
        );

        assertEquals(3, snapshot.bodySize());
    }

    @Test
    void rejectsHttpFailure() {
        assertThrows(IOException.class, () ->
            DelhiveryApacheResponseSnapshot.capture(
                response(500, new byte[] {1}, "application/json"),
                DelhiveryApacheResponseExtractor.Kind.JSON
            )
        );
    }

    @Test
    void rejectsMissingResponse() {
        assertThrows(NullPointerException.class, () ->
            DelhiveryApacheResponseSnapshot.capture(
                null,
                DelhiveryApacheResponseExtractor.Kind.JSON
            )
        );
    }
}