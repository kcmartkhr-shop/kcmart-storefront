package in.kcmart.backend.shipping.delhivery.client;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Objects;

import org.apache.hc.client5.http.DnsResolver;
import org.apache.hc.client5.http.SystemDefaultDnsResolver;

public final class DelhiverySecureDnsResolver implements DnsResolver {

    private final DnsResolver delegate;

    public DelhiverySecureDnsResolver() {
        this(SystemDefaultDnsResolver.INSTANCE);
    }

    public DelhiverySecureDnsResolver(DnsResolver delegate) {
        this.delegate = Objects.requireNonNull(delegate);
    }

    @Override
    public InetAddress[] resolve(String host)
            throws UnknownHostException {

        if (host == null || host.isBlank()) {
            throw new UnknownHostException("DNS hostname required.");
        }

        InetAddress[] addresses = delegate.resolve(host);

        if (addresses == null || addresses.length == 0) {
            throw new UnknownHostException(
                "DNS returned no destination addresses."
            );
        }

        for (InetAddress address : addresses) {
            if (address == null ||
                    !DelhiveryPublicIpValidator.isPublic(address)) {
                throw new UnknownHostException(
                    "DNS returned a disallowed destination address."
                );
            }
        }

        return addresses.clone();
    }

    @Override
    public String resolveCanonicalHostname(String host)
            throws UnknownHostException {
        return delegate.resolveCanonicalHostname(host);
    }
}