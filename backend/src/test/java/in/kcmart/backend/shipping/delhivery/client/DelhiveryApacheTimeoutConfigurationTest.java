package in.kcmart.backend.shipping.delhivery.client;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class DelhiveryApacheTimeoutConfigurationTest {

    private static String source() throws Exception {
        return Files.readString(
            Path.of(
                "src/main/java/in/kcmart/backend/shipping/delhivery/"
                    + "client/DelhiveryApacheHttpClientFactory.java"
            ),
            StandardCharsets.UTF_8
        );
    }

    @Test
    void configuresFiveSecondConnectionTimeout() throws Exception {
        String factory = source();

        assertTrue(
            factory.contains(
                ".setConnectTimeout(Timeout.ofSeconds(5))"
            )
        );
    }

    @Test
    void configuresThreeSecondPoolTimeout() throws Exception {
        String factory = source();

        assertTrue(
            factory.contains(
                ".setConnectionRequestTimeout(Timeout.ofSeconds(3))"
            )
        );
    }

    @Test
    void configuresTwentySecondResponseTimeout() throws Exception {
        String factory = source();

        assertTrue(
            factory.contains(
                ".setResponseTimeout(Timeout.ofSeconds(20))"
            )
        );
    }

    @Test
    void attachesRequestConfigurationToClient() throws Exception {
        String factory = source();

        assertTrue(
            factory.contains(
                ".setDefaultRequestConfig(requestConfig)"
            )
        );
    }

    @Test
    void attachesConnectionConfigurationToManager()
            throws Exception {

        String factory = source();

        assertTrue(
            factory.contains(
                ".setDefaultConnectionConfig("
            )
        );
    }

    @Test
    void preservesDnsTlsAndDirectRouting() throws Exception {
        String factory = source();

        assertAll(
            () -> assertTrue(
                factory.contains(".setDnsResolver(resolver)")
            ),
            () -> assertTrue(
                factory.contains("HostnameVerificationPolicy.BOTH")
            ),
            () -> assertTrue(
                factory.contains(".setRoutePlanner(directRoutes)")
            ),
            () -> assertTrue(
                factory.contains(".disableRedirectHandling()")
            ),
            () -> assertTrue(
                factory.contains(".disableAutomaticRetries()")
            )
        );
    }

    @Test
    void hardenedClientStillConstructs() throws Exception {
        try (var client = DelhiveryApacheHttpClientFactory.create()) {
            assertNotNull(client);
        }
    }
}