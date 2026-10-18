package in.kcmart.backend.payment.razorpay.client;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import in.kcmart.backend.payment.razorpay.config.RazorpayProperties;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class RazorpayRefundLookupTest {

    private HttpServer server;
    private RazorpayClient client;
    private final AtomicInteger requests = new AtomicInteger();
    private final AtomicReference<String> method = new AtomicReference<>();
    private final AtomicReference<String> authorization = new AtomicReference<>();
    private final AtomicReference<String> path = new AtomicReference<>();

    private volatile int statusCode;
    private volatile String responseBody;

    @BeforeEach
    void setup() throws Exception {
        server = HttpServer.create(
                new InetSocketAddress("127.0.0.1", 0), 0
        );

        statusCode = 200;
        responseBody = validResponse("rfnd_test123", "processed");

        server.createContext("/", exchange -> {
            requests.incrementAndGet();
            method.set(exchange.getRequestMethod());
            authorization.set(
                    exchange.getRequestHeaders().getFirst("Authorization")
            );
            path.set(exchange.getRequestURI().getPath());

            reply(exchange, statusCode, responseBody);
        });

        server.start();

        client = new RazorpayClient(
                new RazorpayProperties(
                        "test_key",
                        "test_secret",
                        "unused_webhook_secret",
                        "http://127.0.0.1:" + server.getAddress().getPort()
                )
        );
    }

    @AfterEach
    void cleanup() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void processedRefundUsesAuthenticatedGetOnly() {
        var refund = client.fetchRefund("rfnd_test123");

        assertEquals("rfnd_test123", refund.id());
        assertEquals("pay_test123", refund.payment_id());
        assertEquals(50000L, refund.amount());
        assertEquals("INR", refund.currency());
        assertEquals("processed", refund.status());

        assertEquals(1, requests.get());
        assertEquals("GET", method.get());
        assertEquals("/v1/refunds/rfnd_test123", path.get());

        String expected = "Basic " + Base64.getEncoder()
                .encodeToString(
                        "test_key:test_secret".getBytes(StandardCharsets.UTF_8)
                );

        assertEquals(expected, authorization.get());

        System.out.println("AUTHENTICATED READ-ONLY GET VERIFIED");
    }

    @Test
    void pendingAndFailedResponsesAreNotReportedAsProcessed() {
        for (String gatewayStatus : new String[]{"pending", "failed"}) {
            responseBody = validResponse("rfnd_test123", gatewayStatus);

            var refund = client.fetchRefund("rfnd_test123");

            assertEquals(gatewayStatus, refund.status());
            assertNotEquals("processed", refund.status());
        }

        assertEquals(2, requests.get());

        System.out.println("NON-PROCESSED REFUND STATUSES PRESERVED");
    }

    @Test
    void mismatchedRefundIdIsRejected() {
        responseBody = validResponse("rfnd_different", "processed");

        assertThrows(
                IllegalStateException.class,
                () -> client.fetchRefund("rfnd_test123")
        );
    }

    @Test
    void malformedRefundResponseIsRejected() {
        String[] malformed = {
                "{}",
                "{\"id\":\"rfnd_test123\",\"payment_id\":\"pay_test123\","
                        + "\"amount\":0,\"currency\":\"INR\",\"status\":\"processed\"}",
                "{\"id\":\"rfnd_test123\",\"payment_id\":\"pay_test123\","
                        + "\"amount\":50000,\"currency\":\"INR\"}",
                "{\"id\":\"rfnd_test123\",\"payment_id\":\"\","
                        + "\"amount\":50000,\"currency\":\"INR\",\"status\":\"processed\"}"
        };

        for (String body : malformed) {
            responseBody = body;

            assertThrows(
                    IllegalStateException.class,
                    () -> client.fetchRefund("rfnd_test123")
            );
        }
    }

    @Test
    void gatewayHttpFailureDoesNotProduceVerifiedRefund() {
        statusCode = 503;
        responseBody = "{\"error\":{\"description\":\"unavailable\"}}";

        assertThrows(
                RuntimeException.class,
                () -> client.fetchRefund("rfnd_test123")
        );

        assertEquals("GET", method.get());
        assertEquals(1, requests.get());
    }

    @Test
    void invalidIdsAreRejectedWithoutHttpRequests() {
        String[] invalid = {
                "",
                " ",
                " rfnd_test123",
                "rfnd_test123 ",
                "pay_test123",
                "rfnd_test123/extra",
                "rfnd_test123?x=1"
        };

        for (String id : invalid) {
            assertThrows(
                    IllegalArgumentException.class,
                    () -> client.fetchRefund(id)
            );
        }

        assertThrows(
                IllegalArgumentException.class,
                () -> client.fetchRefund(null)
        );

        assertEquals(0, requests.get());
    }

    @Test
    void missingCredentialsRejectRequestBeforeHttp() {
        var withoutCredentials = new RazorpayClient(
                new RazorpayProperties(
                        "",
                        "",
                        "",
                        "http://127.0.0.1:" + server.getAddress().getPort()
                )
        );

        assertThrows(
                IllegalStateException.class,
                () -> withoutCredentials.fetchRefund("rfnd_test123")
        );

        assertEquals(0, requests.get());
    }

    private static String validResponse(String refundId, String status) {
        return "{"
                + "\"id\":\"" + refundId + "\","
                + "\"entity\":\"refund\","
                + "\"payment_id\":\"pay_test123\","
                + "\"amount\":50000,"
                + "\"currency\":\"INR\","
                + "\"status\":\"" + status + "\","
                + "\"created_at\":1700000000"
                + "}";
    }

    private static void reply(
            HttpExchange exchange,
            int status,
            String body) throws IOException {

        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);

        exchange.getResponseHeaders().set(
                "Content-Type", "application/json"
        );

        try {
            exchange.sendResponseHeaders(status, bytes.length);

            try (var output = exchange.getResponseBody()) {
                output.write(bytes);
            }
        }
        finally {
            exchange.close();
        }
    }
}