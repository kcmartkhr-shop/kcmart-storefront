package in.kcmart.backend.shipping.delhivery.client;

import com.sun.net.httpserver.HttpServer;
import in.kcmart.backend.shipping.delhivery.config.DelhiveryProperties;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class HttpDelhiveryClientErrorTest {

    private void verifyError(
            int status,
            Class<? extends Exception> expectedException
    ) throws Exception {

        HttpServer server = HttpServer.create(
            new InetSocketAddress("127.0.0.1", 0), 0
        );

        server.createContext("/", exchange -> {
            byte[] body = "{\"error\":\"mock failure\"}"
                .getBytes(StandardCharsets.UTF_8);

            exchange.getResponseHeaders().set(
                "Content-Type", "application/json"
            );

            exchange.sendResponseHeaders(status, body.length);

            try (var output = exchange.getResponseBody()) {
                output.write(body);
            }
        });

        server.start();

        try {
            DelhiveryProperties properties =
                new DelhiveryProperties(
                    true,
                    "http://127.0.0.1:" + server.getAddress().getPort(),
                    "mock-test-token",
                    "MOCK-PICKUP"
                );

            DelhiveryClient client = new HttpDelhiveryClient(
                RestClient.builder(),
                properties
            );

            assertThrows(
                expectedException,
                () -> client.trackShipment("TEST-AWB-123")
            );

        } finally {
            server.stop(0);
        }
    }

    @Test
    void unauthorizedResponseIsRejected() throws Exception {
        verifyError(
            401,
            HttpClientErrorException.class
        );
    }

    @Test
    void serverFailureIsRejected() throws Exception {
        verifyError(
            500,
            HttpServerErrorException.class
        );
    }
}