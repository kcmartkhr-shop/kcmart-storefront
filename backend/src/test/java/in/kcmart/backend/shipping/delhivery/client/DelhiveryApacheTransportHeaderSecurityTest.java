package in.kcmart.backend.shipping.delhivery.client;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;

import static org.junit.jupiter.api.Assertions.*;

class DelhiveryApacheTransportHeaderSecurityTest {

    private static HttpRequest.Builder builder(String url) {
        return HttpRequest.newBuilder(URI.create(url));
    }

    @Test
    void acceptsValidApiHeaders() {
        var request = builder(
            "https://track.delhivery.com/api/p/packing_slip"
        )
            .header("Accept", "application/json")
            .header("Authorization", "Token test-token")
            .GET()
            .build();

        assertDoesNotThrow(() ->
            DelhiveryApacheTransportAdapter.validateRequest(request)
        );
    }

    @Test
    void acceptsValidPdfHeaders() {
        var request = builder(
            "https://s3.amazonaws.com/test.pdf"
        )
            .header("Accept", "application/pdf")
            .GET()
            .build();

        assertDoesNotThrow(() ->
            DelhiveryApacheTransportAdapter.validateRequest(request)
        );
    }

    @Test
    void rejectsAuthorizationOnPdfHost() {
        var request = builder(
            "https://s3.amazonaws.com/test.pdf"
        )
            .header("Accept", "application/pdf")
            .header("Authorization", "Token test-token")
            .GET()
            .build();

        assertThrows(IOException.class, () ->
            DelhiveryApacheTransportAdapter.validateRequest(request)
        );
    }

    @Test
    void rejectsCookie() {
        var request = builder(
            "https://track.delhivery.com/api/p/packing_slip"
        )
            .header("Accept", "application/json")
            .header("Authorization", "Token test-token")
            .header("Cookie", "session=abc")
            .GET()
            .build();

        assertThrows(IOException.class, () ->
            DelhiveryApacheTransportAdapter.validateRequest(request)
        );
    }

    @Test
    void rejectsProxyAuthorization() {
        var request = builder(
            "https://s3.amazonaws.com/test.pdf"
        )
            .header("Accept", "application/pdf")
            .header("Proxy-Authorization", "Basic test")
            .GET()
            .build();

        assertThrows(IOException.class, () ->
            DelhiveryApacheTransportAdapter.validateRequest(request)
        );
    }

    @Test
    void rejectsWrongAcceptHeader() {
        var request = builder(
            "https://s3.amazonaws.com/test.pdf"
        )
            .header("Accept", "application/json")
            .GET()
            .build();

        assertThrows(IOException.class, () ->
            DelhiveryApacheTransportAdapter.validateRequest(request)
        );
    }

    @Test
    void rejectsMissingAcceptHeader() {
        var request = builder(
            "https://s3.amazonaws.com/test.pdf"
        )
            .GET()
            .build();

        assertThrows(IOException.class, () ->
            DelhiveryApacheTransportAdapter.validateRequest(request)
        );
    }

    @Test
    void rejectsMissingApiAuthorization() {
        var request = builder(
            "https://track.delhivery.com/api/p/packing_slip"
        )
            .header("Accept", "application/json")
            .GET()
            .build();

        assertThrows(IOException.class, () ->
            DelhiveryApacheTransportAdapter.validateRequest(request)
        );
    }

    @Test
    void rejectsUnknownCustomHeader() {
        var request = builder(
            "https://s3.amazonaws.com/test.pdf"
        )
            .header("Accept", "application/pdf")
            .header("X-Custom", "unexpected")
            .GET()
            .build();

        assertThrows(IOException.class, () ->
            DelhiveryApacheTransportAdapter.validateRequest(request)
        );
    }

    @Test
    void executionRemainsDisabledWithValidHeaders() {
        var request = builder(
            "https://s3.amazonaws.com/test.pdf"
        )
            .header("Accept", "application/pdf")
            .GET()
            .build();

        assertThrows(IOException.class, () ->
            DelhiveryApacheTransportAdapter.sendDisabled(request)
        );
    }
}