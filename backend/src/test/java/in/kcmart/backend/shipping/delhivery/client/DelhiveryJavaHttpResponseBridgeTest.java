package in.kcmart.backend.shipping.delhivery.client;

import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpRequest;
import java.nio.charset.StandardCharsets;

import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.entity.ByteArrayEntity;
import org.apache.hc.core5.http.message.BasicClassicHttpResponse;

import static org.junit.jupiter.api.Assertions.*;

class DelhiveryJavaHttpResponseBridgeTest {

    private static HttpRequest request() {
        return HttpRequest.newBuilder(
            URI.create("https://s3.amazonaws.com/test.pdf")
        )
            .header("Accept", "application/pdf")
            .GET()
            .build();
    }

    private static DelhiveryApacheResponseSnapshot snapshot()
            throws Exception {

        var response = new BasicClassicHttpResponse(200);

        response.setEntity(
            new ByteArrayEntity(
                new byte[] {1, 2, 3},
                ContentType.APPLICATION_OCTET_STREAM
            )
        );

        response.setHeader("Content-Type", "application/pdf");

        return DelhiveryApacheResponseSnapshot.capture(
            response,
            DelhiveryApacheResponseExtractor.Kind.PDF
        );
    }

    @Test
    void preservesStatus() throws Exception {
        var bridged = DelhiveryJavaHttpResponseBridge.fromSnapshot(
            request(), snapshot()
        );

        assertEquals(200, bridged.statusCode());
    }

    @Test
    void preservesRequest() throws Exception {
        var request = request();

        var bridged = DelhiveryJavaHttpResponseBridge.fromSnapshot(
            request, snapshot()
        );

        assertSame(request, bridged.request());
    }

    @Test
    void preservesUri() throws Exception {
        var bridged = DelhiveryJavaHttpResponseBridge.fromSnapshot(
            request(), snapshot()
        );

        assertEquals(
            URI.create("https://s3.amazonaws.com/test.pdf"),
            bridged.uri()
        );
    }

    @Test
    void exposesBodyStream() throws Exception {
        var bridged = DelhiveryJavaHttpResponseBridge.fromSnapshot(
            request(), snapshot()
        );

        try (var body = bridged.body()) {
            assertArrayEquals(
                new byte[] {1, 2, 3},
                body.readAllBytes()
            );
        }
    }

    @Test
    void providesIndependentBodyStreams() throws Exception {
        var bridged = DelhiveryJavaHttpResponseBridge.fromSnapshot(
            request(), snapshot()
        );

        try (var first = bridged.body();
             var second = bridged.body()) {

            assertEquals(1, first.read());
            assertEquals(1, second.read());
        }
    }

    @Test
    void preservesSafeContentType() throws Exception {
        var bridged = DelhiveryJavaHttpResponseBridge.fromSnapshot(
            request(), snapshot()
        );

        assertEquals(
            "application/pdf",
            bridged.headers()
                .firstValue("Content-Type")
                .orElseThrow()
        );
    }

    @Test
    void excludesSensitiveHeaders() throws Exception {
        var bridged = DelhiveryJavaHttpResponseBridge.fromSnapshot(
            request(), snapshot()
        );

        assertTrue(
            bridged.headers().firstValue("Set-Cookie").isEmpty()
        );

        assertTrue(
            bridged.headers().firstValue("Authorization").isEmpty()
        );
    }

    @Test
    void hasNoPreviousResponse() throws Exception {
        var bridged = DelhiveryJavaHttpResponseBridge.fromSnapshot(
            request(), snapshot()
        );

        assertTrue(bridged.previousResponse().isEmpty());
    }

    @Test
    void reportsHttp11Version() throws Exception {
        var bridged = DelhiveryJavaHttpResponseBridge.fromSnapshot(
            request(), snapshot()
        );

        assertEquals(
            java.net.http.HttpClient.Version.HTTP_1_1,
            bridged.version()
        );
    }

    @Test
    void rejectsNullRequest() throws Exception {
        assertThrows(NullPointerException.class, () ->
            DelhiveryJavaHttpResponseBridge.fromSnapshot(
                null, snapshot()
            )
        );
    }

    @Test
    void rejectsNullSnapshot() {
        assertThrows(NullPointerException.class, () ->
            DelhiveryJavaHttpResponseBridge.fromSnapshot(
                request(), null
            )
        );
    }
}