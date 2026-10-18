package in.kcmart.backend.shipping.delhivery.client;

import com.sun.net.httpserver.HttpServer;
import in.kcmart.backend.shipping.delhivery.config.DelhiveryProperties;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.net.InetSocketAddress;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class HttpDelhiveryClientReadTimeoutTest {

    @Test
    void slowResponseTriggersReadTimeout() throws Exception {

        HttpServer server = HttpServer.create(
            new InetSocketAddress("127.0.0.1", 0), 0
        );

        var executor = Executors.newSingleThreadExecutor();

        server.setExecutor(executor);

        server.createContext("/", exchange -> {
            try {
                Thread.sleep(20000);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
            } finally {
                exchange.close();
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

            long start = System.nanoTime();

            ResourceAccessException exception = assertThrows(
                ResourceAccessException.class,
                () -> client.trackShipment("TEST-AWB-123")
            );

            long elapsed = TimeUnit.NANOSECONDS.toSeconds(
                System.nanoTime() - start
            );

            assertTrue(
                elapsed >= 13 && elapsed < 19,
                "Expected timeout near 15 seconds, actual: "
                    + elapsed + " seconds"
            );

            assertNotNull(exception.getCause());

        } finally {
            server.stop(0);
            executor.shutdownNow();
        }
    }
}