package in.kcmart.backend.shipping.delhivery.client;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.util.Objects;

public final class DelhiveryPublicIpValidator {

    private DelhiveryPublicIpValidator() {
    }

    public static boolean isPublic(InetAddress address) {
        Objects.requireNonNull(address, "IP address required");

        if (address.isAnyLocalAddress()
                || address.isLoopbackAddress()
                || address.isLinkLocalAddress()
                || address.isSiteLocalAddress()
                || address.isMulticastAddress()) {
            return false;
        }

        byte[] bytes = address.getAddress();

        if (address instanceof Inet4Address) {
            int a = bytes[0] & 0xff;
            int b = bytes[1] & 0xff;
            int c = bytes[2] & 0xff;

            if (a == 0 || a == 10 || a == 127 || a >= 224) {
                return false;
            }

            if (a == 100 && b >= 64 && b <= 127) {
                return false;
            }

            if (a == 169 && b == 254) {
                return false;
            }

            if (a == 172 && b >= 16 && b <= 31) {
                return false;
            }

            if (a == 192 && b == 168) {
                return false;
            }

            if (a == 192 && b == 0 && c == 0) {
                return false;
            }

            if (a == 192 && b == 0 && c == 2) {
                return false;
            }

            if (a == 192 && b == 88 && c == 99) {
                return false;
            }

            if (a == 198 && (b == 18 || b == 19)) {
                return false;
            }

            if (a == 198 && b == 51 && c == 100) {
                return false;
            }

            if (a == 203 && b == 0 && c == 113) {
                return false;
            }

            return true;
        }

        if (address instanceof Inet6Address) {
            int first = bytes[0] & 0xff;
            int second = bytes[1] & 0xff;

            // Accept only ordinary global-unicast candidates.
            // This excludes ULA, link-local, multicast,
            // documentation and IPv4-mapped ranges.
            if ((first & 0xe0) != 0x20) {
                return false;
            }

            // IPv6 documentation prefix 2001:db8::/32.
            if (first == 0x20
                    && second == 0x01
                    && (bytes[2] & 0xff) == 0x0d
                    && (bytes[3] & 0xff) == 0xb8) {
                return false;
            }

            return true;
        }

        return false;
    }

    public static void requirePublic(InetAddress address) {
        if (!isPublic(address)) {
            throw new IllegalArgumentException(
                "Non-public destination IP address rejected."
            );
        }
    }
}