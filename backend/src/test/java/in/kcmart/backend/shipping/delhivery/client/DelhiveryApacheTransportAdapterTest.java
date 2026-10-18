package in.kcmart.backend.shipping.delhivery.client;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;

import static org.junit.jupiter.api.Assertions.*;

class DelhiveryApacheTransportAdapterTest {

    private static HttpRequest request(String url) {
        var builder = HttpRequest.newBuilder(URI.create(url));

        String host = URI.create(url).getHost();

        if ("track.delhivery.com".equals(host) ||
                "staging-express.delhivery.com".equals(host)) {
            builder.header("Accept", "application/json");
            builder.header("Authorization", "Token test-token");
        } else {
            builder.header("Accept", "application/pdf");
        }

        return builder.GET().build();
    }

    @Test
    void acceptsApprovedDelhiveryHost() throws Exception {
        var uri = DelhiveryApacheTransportAdapter.validateRequest(
            request("https://track.delhivery.com/api/p/packing_slip")
        );

        assertEquals("track.delhivery.com", uri.getHost());
    }

    @Test
    void acceptsApprovedStagingHost() throws Exception {
        assertDoesNotThrow(() ->
            DelhiveryApacheTransportAdapter.validateRequest(
                request(
                    "https://staging-express.delhivery.com/api/p/packing_slip"
                )
            )
        );
    }

    @Test
    void acceptsApprovedPdfHost() throws Exception {
        assertDoesNotThrow(() ->
            DelhiveryApacheTransportAdapter.validateRequest(
                request("https://s3.amazonaws.com/test-label.pdf")
            )
        );
    }

    @Test
    void rejectsHttp() {
        assertThrows(IOException.class, () ->
            DelhiveryApacheTransportAdapter.validateRequest(
                request("http://s3.amazonaws.com/test.pdf")
            )
        );
    }

    @Test
    void rejectsUnapprovedHost() {
        assertThrows(IOException.class, () ->
            DelhiveryApacheTransportAdapter.validateRequest(
                request("https://example.com/test.pdf")
            )
        );
    }

    @Test
    void rejectsCustomPort() {
        assertThrows(IOException.class, () ->
            DelhiveryApacheTransportAdapter.validateRequest(
                request("https://s3.amazonaws.com:8443/test.pdf")
            )
        );
    }

    @Test
    void rejectsPost() {
        var post = HttpRequest.newBuilder(
                URI.create("https://s3.amazonaws.com/test.pdf")
            )
            .POST(HttpRequest.BodyPublishers.noBody())
            .build();

        assertThrows(IOException.class, () ->
            DelhiveryApacheTransportAdapter.validateRequest(post)
        );
    }

    @Test
    void rejectsMissingRequest() {
        assertThrows(IOException.class, () ->
            DelhiveryApacheTransportAdapter.validateRequest(null)
        );
    }

    @Test
    void executionRemainsDisabled() {
        assertThrows(IOException.class, () ->
            DelhiveryApacheTransportAdapter.sendDisabled(
                request("https://s3.amazonaws.com/test.pdf")
            )
        );
    }
}