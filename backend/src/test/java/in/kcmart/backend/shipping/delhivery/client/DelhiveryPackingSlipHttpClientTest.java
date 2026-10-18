package in.kcmart.backend.shipping.delhivery.client;

import org.junit.jupiter.api.Test;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.http.HttpHeaders;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DelhiveryPackingSlipHttpClientTest {

    private static final String BASE =
        "https://staging-express.delhivery.com";

    @SuppressWarnings("unchecked")
    private HttpResponse<InputStream> response(
            int status, byte[] body, long declaredLength) {

        HttpResponse<InputStream> response =
            mock(HttpResponse.class);

        HttpHeaders headers = HttpHeaders.of(
            Map.of(
                "Content-Length",
                List.of(String.valueOf(declaredLength))
            ),
            (name, value) -> true
        );

        when(response.statusCode()).thenReturn(status);
        when(response.headers()).thenReturn(headers);
        when(response.body()).thenReturn(
            new ByteArrayInputStream(body)
        );

        return response;
    }

    private byte[] bytes(String value) {
        return value.getBytes(StandardCharsets.UTF_8);
    }

    @Test
    void acceptsMockJson() throws Exception {
        String json = "{\"sample_link\":\"example\"}";

        var client = new DelhiveryPackingSlipHttpClient(
            request -> {
                assertEquals("GET", request.method());
                assertEquals(
                    "staging-express.delhivery.com",
                    request.uri().getHost()
                );
                return response(200, bytes(json), bytes(json).length);
            }
        );

        assertEquals(
            json,
            client.fetchJson(BASE, "test-token", "123456789012")
        );
    }

    @Test
    void rejectsRedirect() {
        var client = new DelhiveryPackingSlipHttpClient(
            request -> response(302, bytes("redirect"), 8)
        );

        assertThrows(
            IOException.class,
            () -> client.fetchJson(
                BASE, "test-token", "123456789012"
            )
        );
    }

    @Test
    void rejectsServerError() {
        var client = new DelhiveryPackingSlipHttpClient(
            request -> response(500, bytes("error"), 5)
        );

        assertThrows(
            IOException.class,
            () -> client.fetchJson(
                BASE, "test-token", "123456789012"
            )
        );
    }

    @Test
    void rejectsOversizedDeclaredBody() {
        var client = new DelhiveryPackingSlipHttpClient(
            request -> response(
                200, bytes("{}"), 256L * 1024 + 1
            )
        );

        assertThrows(
            IOException.class,
            () -> client.fetchJson(
                BASE, "test-token", "123456789012"
            )
        );
    }

    @Test
    void rejectsOversizedActualBody() {
        byte[] large = new byte[256 * 1024 + 1];

        var client = new DelhiveryPackingSlipHttpClient(
            request -> response(200, large, -1)
        );

        assertThrows(
            IOException.class,
            () -> client.fetchJson(
                BASE, "test-token", "123456789012"
            )
        );
    }

    @Test
    void rejectsEmptyBody() {
        var client = new DelhiveryPackingSlipHttpClient(
            request -> response(200, new byte[0], 0)
        );

        assertThrows(
            IOException.class,
            () -> client.fetchJson(
                BASE, "test-token", "123456789012"
            )
        );
    }

    @Test
    void rejectsUnsafeBaseBeforeTransport() {
        var transport =
            mock(DelhiveryPackingSlipHttpClient.Transport.class);

        var client = new DelhiveryPackingSlipHttpClient(
            transport
        );

        assertThrows(
            IllegalArgumentException.class,
            () -> client.fetchJson(
                "http://localhost",
                "test-token",
                "123456789012"
            )
        );

        verifyNoInteractions(transport);
    }

    @Test
    void doesNotIncludeTokenInUrl() throws Exception {
        var client = new DelhiveryPackingSlipHttpClient(
            request -> {
                assertFalse(
                    request.uri().toString().contains("test-token")
                );
                assertEquals(
                    "Token test-token",
                    request.headers()
                        .firstValue("Authorization")
                        .orElseThrow()
                );

                return response(200, bytes("{}"), 2);
            }
        );

        assertEquals(
            "{}",
            client.fetchJson(
                BASE, "test-token", "123456789012"
            )
        );
    }
}