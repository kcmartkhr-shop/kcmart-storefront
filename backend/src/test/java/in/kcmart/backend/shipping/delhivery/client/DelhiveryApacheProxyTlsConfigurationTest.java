package in.kcmart.backend.shipping.delhivery.client;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class DelhiveryApacheProxyTlsConfigurationTest {

    private static String factorySource() throws Exception {
        Path path = Path.of(
            "src/main/java/in/kcmart/backend/shipping/delhivery/client/"
                + "DelhiveryApacheHttpClientFactory.java"
        );

        return Files.readString(path, StandardCharsets.UTF_8);
    }

    @Test
    void configuresExplicitHostnameVerification() throws Exception {
        String source = factorySource();

        assertTrue(
            source.contains("HostnameVerificationPolicy.BOTH")
        );

        assertTrue(
            source.contains(".setTlsSocketStrategy(")
        );
    }

    @Test
    void configuresDirectRoutePlanner() throws Exception {
        String source = factorySource();

        assertTrue(
            source.contains("new DefaultRoutePlanner(null)")
        );

        assertTrue(
            source.contains(".setRoutePlanner(directRoutes)")
        );
    }

    @Test
    void disablesRedirectsAndRetries() throws Exception {
        String source = factorySource();

        assertTrue(
            source.contains(".disableRedirectHandling()")
        );

        assertTrue(
            source.contains(".disableAutomaticRetries()")
        );
    }

    @Test
    void doesNotEnableSystemProperties() throws Exception {
        String source = factorySource();

        assertFalse(
            source.contains(".useSystemProperties()")
        );
    }

    @Test
    void configuresSecureDnsAtConnectionManager() throws Exception {
        String source = factorySource();

        assertTrue(
            source.contains(".setDnsResolver(resolver)")
        );

        assertTrue(
            source.contains(
                "return create(new DelhiverySecureDnsResolver())"
            )
        );
    }

    @Test
    void createsAndClosesHardenedClient() throws Exception {
        try (var client = DelhiveryApacheHttpClientFactory.create()) {
            assertNotNull(client);
        }
    }
}