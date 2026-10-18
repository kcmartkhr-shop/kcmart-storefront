package in.kcmart.backend.shipping.delhivery.client;

import org.apache.hc.client5.http.DnsResolver;
import org.junit.jupiter.api.Test;

import java.net.InetAddress;
import java.net.UnknownHostException;

import static org.junit.jupiter.api.Assertions.*;

class DelhiverySecureDnsResolverTest {

    private static InetAddress ip(String address) throws Exception {
        return InetAddress.getByName(address);
    }

    private static DnsResolver fake(InetAddress... addresses) {
        return new DnsResolver() {
            @Override
            public InetAddress[] resolve(String host) {
                return addresses;
            }

            @Override
            public String resolveCanonicalHostname(String host) {
                return host;
            }
        };
    }

    @Test
    void acceptsPublicIpv4() throws Exception {
        var resolver = new DelhiverySecureDnsResolver(
            fake(ip("8.8.8.8"))
        );

        assertEquals(
            "8.8.8.8",
            resolver.resolve("example.com")[0].getHostAddress()
        );
    }

    @Test
    void acceptsPublicIpv6() throws Exception {
        var resolver = new DelhiverySecureDnsResolver(
            fake(ip("2606:4700:4700::1111"))
        );

        assertEquals(
            1,
            resolver.resolve("example.com").length
        );
    }

    @Test
    void rejectsLoopback() throws Exception {
        var resolver = new DelhiverySecureDnsResolver(
            fake(ip("127.0.0.1"))
        );

        assertThrows(
            UnknownHostException.class,
            () -> resolver.resolve("example.com")
        );
    }

    @Test
    void rejectsPrivateIpv4() throws Exception {
        var resolver = new DelhiverySecureDnsResolver(
            fake(ip("10.0.0.5"))
        );

        assertThrows(
            UnknownHostException.class,
            () -> resolver.resolve("example.com")
        );
    }

    @Test
    void rejectsMixedPublicAndPrivateAnswers() throws Exception {
        var resolver = new DelhiverySecureDnsResolver(
            fake(ip("8.8.8.8"), ip("192.168.1.10"))
        );

        assertThrows(
            UnknownHostException.class,
            () -> resolver.resolve("example.com")
        );
    }

    @Test
    void rejectsEmptyDnsAnswers() {
        var resolver = new DelhiverySecureDnsResolver(fake());

        assertThrows(
            UnknownHostException.class,
            () -> resolver.resolve("example.com")
        );
    }

    @Test
    void rejectsNullDnsAnswer() {
        var resolver = new DelhiverySecureDnsResolver(
            fake((InetAddress[]) null)
        );

        assertThrows(
            UnknownHostException.class,
            () -> resolver.resolve("example.com")
        );
    }

    @Test
    void rejectsNullAddressInDnsAnswer() throws Exception {
        var resolver = new DelhiverySecureDnsResolver(
            fake(ip("8.8.8.8"), null)
        );

        assertThrows(
            UnknownHostException.class,
            () -> resolver.resolve("example.com")
        );
    }

    @Test
    void rejectsBlankHostname() {
        var resolver = new DelhiverySecureDnsResolver(fake());

        assertThrows(
            UnknownHostException.class,
            () -> resolver.resolve(" ")
        );
    }

    @Test
    void returnsDefensiveCopy() throws Exception {
        InetAddress[] original = { ip("8.8.8.8") };

        var resolver = new DelhiverySecureDnsResolver(
            fake(original)
        );

        InetAddress[] result = resolver.resolve("example.com");

        assertNotSame(original, result);
    }
}