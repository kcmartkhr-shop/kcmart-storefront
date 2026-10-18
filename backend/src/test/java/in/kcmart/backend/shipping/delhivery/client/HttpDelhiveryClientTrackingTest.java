package in.kcmart.backend.shipping.delhivery.client;

import com.sun.net.httpserver.HttpServer;
import in.kcmart.backend.shipping.delhivery.config.DelhiveryProperties;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class HttpDelhiveryClientTrackingTest {

    @Test
    void trackingUsesCorrectEndpointAndToken() throws Exception {
        AtomicReference<String> path = new AtomicReference<>();
        AtomicReference<String> query = new AtomicReference<>();
        AtomicReference<String> authorization = new AtomicReference<>();

        HttpServer server = HttpServer.create(
            new InetSocketAddress("127.0.0.1", 0), 0
        );

        server.createContext("/", exchange -> {
            path.set(exchange.getRequestURI().getPath());
            query.set(exchange.getRequestURI().getRawQuery());
            authorization.set(
                exchange.getRequestHeaders().getFirst("Authorization")
            );

            byte[] body = "{\"ShipmentData\":[]}"
                .getBytes(StandardCharsets.UTF_8);

            exchange.getResponseHeaders().set(
                "Content-Type", "application/json"
            );
            exchange.sendResponseHeaders(200, body.length);

            try (var output = exchange.getResponseBody()) {
                output.write(body);
            }
        });

        server.start();

        try {
            String baseUrl = "http://127.0.0.1:"
                + server.getAddress().getPort();

            DelhiveryProperties properties =
                new DelhiveryProperties(
                    true,
                    baseUrl,
                    "mock-test-token",
                    "MOCK-PICKUP"
                );

            DelhiveryClient client = new HttpDelhiveryClient(
                RestClient.builder(),
                properties
            );

            String response = client.trackShipment("TEST-AWB-123");

            assertEquals(
                "/api/v1/packages/json/",
                path.get()
            );
            assertEquals(
                "waybill=TEST-AWB-123",
                query.get()
            );
            assertEquals(
                "Token mock-test-token",
                authorization.get()
            );
            assertEquals(
                "{\"ShipmentData\":[]}",
                response
            );
        } finally {
            server.stop(0);
        }
    }

    @Test
    void invalidWaybillIsRejectedBeforeNetworkCall() {
        DelhiveryProperties properties =
            new DelhiveryProperties(
                true,
                "http://127.0.0.1:1",
                "mock-test-token",
                "MOCK-PICKUP"
            );

        DelhiveryClient client = new HttpDelhiveryClient(
            RestClient.builder(),
            properties
        );

        assertThrows(
            IllegalArgumentException.class,
            () -> client.trackShipment("../invalid")
        );
    }

    @Test
    void missingTokenIsRejectedBeforeNetworkCall() {
        DelhiveryProperties properties =
            new DelhiveryProperties(
                true,
                "http://127.0.0.1:1",
                "",
                "MOCK-PICKUP"
            );

        DelhiveryClient client = new HttpDelhiveryClient(
            RestClient.builder(),
            properties
        );

        assertThrows(
            IllegalStateException.class,
            () -> client.trackShipment("TEST-AWB-123")
        );
    }
}