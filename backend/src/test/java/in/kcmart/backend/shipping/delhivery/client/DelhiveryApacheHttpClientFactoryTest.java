package in.kcmart.backend.shipping.delhivery.client;

import org.apache.hc.client5.http.DnsResolver;
import org.junit.jupiter.api.Test;

import java.net.InetAddress;

import static org.junit.jupiter.api.Assertions.*;

class DelhiveryApacheHttpClientFactoryTest {

    @Test
    void createsClientWithoutNetworkCall() throws Exception {
        try (var client = DelhiveryApacheHttpClientFactory.create()) {
            assertNotNull(client);
        }
    }

    @Test
    void acceptsInjectedDnsResolverWithoutCallingIt() throws Exception {
        DnsResolver resolver = new DnsResolver() {
            @Override
            public InetAddress[] resolve(String host) {
                throw new AssertionError(
                    "DNS must not run during client construction"
                );
            }

            @Override
            public String resolveCanonicalHostname(String host) {
                throw new AssertionError(
                    "Canonical DNS must not run during construction"
                );
            }
        };

        try (var client =
                DelhiveryApacheHttpClientFactory.create(resolver)) {
            assertNotNull(client);
        }
    }

    @Test
    void rejectsNullDnsResolver() {
        assertThrows(
            NullPointerException.class,
            () -> DelhiveryApacheHttpClientFactory.create(null)
        );
    }
}