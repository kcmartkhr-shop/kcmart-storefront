package in.kcmart.backend.shipping.delhivery.client;

import org.apache.hc.client5.http.DnsResolver;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.junit.jupiter.api.Test;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class DelhiveryApacheConnectionSecurityTest {

    private static DnsResolver fakeDns(String address) throws Exception {
        InetAddress ip = InetAddress.getByName(address);

        return new DnsResolver() {
            @Override
            public InetAddress[] resolve(String host) {
                return new InetAddress[] { ip };
            }

            @Override
            public String resolveCanonicalHostname(String host) {
                return host;
            }
        };
    }

    @Test
    void rejectsLoopbackBeforeConnecting() throws Exception {
        var secureResolver = new DelhiverySecureDnsResolver(
            fakeDns("127.0.0.1")
        );

        try (var client =
                DelhiveryApacheHttpClientFactory.create(secureResolver)) {

            var request = new HttpGet(
                "https://s3.amazonaws.com/test-label.pdf"
            );

            assertThrows(
                IOException.class,
                () -> client.execute(request, response -> {
                    throw new AssertionError(
                        "HTTP response must never be reached"
                    );
                })
            );
        }
    }

    @Test
    void rejectsPrivateNetworkBeforeConnecting() throws Exception {
        var secureResolver = new DelhiverySecureDnsResolver(
            fakeDns("192.168.1.20")
        );

        try (var client =
                DelhiveryApacheHttpClientFactory.create(secureResolver)) {

            var request = new HttpGet(
                "https://s3.amazonaws.com/test-label.pdf"
            );

            assertThrows(
                IOException.class,
                () -> client.execute(request, response -> {
                    throw new AssertionError(
                        "HTTP response must never be reached"
                    );
                })
            );
        }
    }

    @Test
    void rejectsMixedDnsAnswersBeforeConnecting() throws Exception {
        DnsResolver mixed = new DnsResolver() {
            @Override
            public InetAddress[] resolve(String host)
                    throws UnknownHostException {
                return new InetAddress[] {
                    InetAddress.getByName("8.8.8.8"),
                    InetAddress.getByName("10.0.0.10")
                };
            }

            @Override
            public String resolveCanonicalHostname(String host) {
                return host;
            }
        };

        var secureResolver = new DelhiverySecureDnsResolver(mixed);

        try (var client =
                DelhiveryApacheHttpClientFactory.create(secureResolver)) {

            var request = new HttpGet(
                "https://s3.amazonaws.com/test-label.pdf"
            );

            assertThrows(
                IOException.class,
                () -> client.execute(request, response -> {
                    throw new AssertionError(
                        "HTTP response must never be reached"
                    );
                })
            );
        }
    }
}