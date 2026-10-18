package in.kcmart.backend.shipping.delhivery.client;

import org.junit.jupiter.api.Test;
import java.net.InetAddress;
import static org.junit.jupiter.api.Assertions.*;

class DelhiveryPublicIpValidatorTest {

    private boolean allowed(String ip) throws Exception {
        return DelhiveryPublicIpValidator.isPublic(
            InetAddress.getByName(ip)
        );
    }

    @Test
    void acceptsPublicIpv4() throws Exception {
        assertTrue(allowed("8.8.8.8"));
    }

    @Test
    void rejectsLoopback() throws Exception {
        assertFalse(allowed("127.0.0.1"));
    }

    @Test
    void rejectsPrivateIpv4() throws Exception {
        assertFalse(allowed("10.1.2.3"));
        assertFalse(allowed("172.16.0.1"));
        assertFalse(allowed("192.168.1.1"));
    }

    @Test
    void rejectsLinkLocal() throws Exception {
        assertFalse(allowed("169.254.169.254"));
    }

    @Test
    void rejectsCarrierGradeNat() throws Exception {
        assertFalse(allowed("100.64.0.1"));
    }

    @Test
    void rejectsDocumentationIpv4() throws Exception {
        assertFalse(allowed("192.0.2.1"));
        assertFalse(allowed("198.51.100.1"));
        assertFalse(allowed("203.0.113.1"));
    }

    @Test
    void rejectsBenchmarkIpv4() throws Exception {
        assertFalse(allowed("198.18.0.1"));
    }

    @Test
    void rejectsLoopbackIpv6() throws Exception {
        assertFalse(allowed("::1"));
    }

    @Test
    void rejectsUniqueLocalIpv6() throws Exception {
        assertFalse(allowed("fd00::1"));
    }

    @Test
    void rejectsDocumentationIpv6() throws Exception {
        assertFalse(allowed("2001:db8::1"));
    }

    @Test
    void acceptsPublicIpv6Candidate() throws Exception {
        assertTrue(allowed("2606:4700:4700::1111"));
    }

    @Test
    void rejectsNull() {
        assertThrows(
            NullPointerException.class,
            () -> DelhiveryPublicIpValidator.isPublic(null)
        );
    }
}