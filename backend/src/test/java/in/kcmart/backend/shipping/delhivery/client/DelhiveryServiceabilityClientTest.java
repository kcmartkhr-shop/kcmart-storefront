package in.kcmart.backend.shipping.delhivery.client;

import com.sun.net.httpserver.HttpServer;
import in.kcmart.backend.shipping.delhivery.config.DelhiveryProperties;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class DelhiveryServiceabilityClientTest {

    @Test
    void sendsCorrectPincodeRequest() throws Exception {

        AtomicReference<String> path = new AtomicReference<>();
        AtomicReference<String> query = new AtomicReference<>();
        AtomicReference<String> auth = new AtomicReference<>();

        HttpServer server = HttpServer.create(
            new InetSocketAddress("127.0.0.1", 0), 0
        );

        server.createContext("/", exchange -> {
            path.set(exchange.getRequestURI().getPath());
            query.set(exchange.getRequestURI().getRawQuery());
            auth.set(
                exchange.getRequestHeaders().getFirst("Authorization")
            );

            byte[] response = "[]".getBytes(StandardCharsets.UTF_8);

            exchange.getResponseHeaders().set(
                "Content-Type", "application/json"
            );
            exchange.sendResponseHeaders(200, response.length);

            try (var output = exchange.getResponseBody()) {
                output.write(response);
            }
        });

        server.start();

        try {
            DelhiveryProperties properties = new DelhiveryProperties(
                true,
                "http://127.0.0.1:" + server.getAddress().getPort(),
                "mock-token",
                "MOCK-PICKUP"
            );

            DelhiveryServiceabilityClient client =
                new DelhiveryServiceabilityClient(
                    RestClient.builder(), properties
                );

            assertEquals("[]", client.fetchPincode("194103"));
            assertEquals("/c/api/pin-codes/json/", path.get());
            assertEquals("filter_codes=194103", query.get());
            assertEquals("Token mock-token", auth.get());
        } finally {
            server.stop(0);
        }
    }

    @Test
    void disabledIntegrationBlocksRequest() {
        DelhiveryServiceabilityClient client = createClient(
            false, "mock-token"
        );

        assertThrows(
            IllegalStateException.class,
            () -> client.fetchPincode("194103")
        );
    }

    @Test
    void invalidPincodeIsRejected() {
        DelhiveryServiceabilityClient client = createClient(
            true, "mock-token"
        );

        assertThrows(
            IllegalArgumentException.class,
            () -> client.fetchPincode("../invalid")
        );
    }

    @Test
    void missingTokenIsRejected() {
        DelhiveryServiceabilityClient client = createClient(
            true, ""
        );

        assertThrows(
            IllegalStateException.class,
            () -> client.fetchPincode("194103")
        );
    }

    private DelhiveryServiceabilityClient createClient(
            boolean enabled, String token) {

        DelhiveryProperties properties = new DelhiveryProperties(
            enabled,
            "http://127.0.0.1:1",
            token,
            "MOCK-PICKUP"
        );

        return new DelhiveryServiceabilityClient(
            RestClient.builder(), properties
        );
    }
}