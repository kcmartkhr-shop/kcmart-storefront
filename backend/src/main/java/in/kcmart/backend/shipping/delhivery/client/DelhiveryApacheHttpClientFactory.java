package in.kcmart.backend.shipping.delhivery.client;

import java.util.Objects;

import org.apache.hc.client5.http.DnsResolver;
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.core5.util.Timeout;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.routing.HttpRoutePlanner;
import org.apache.hc.client5.http.impl.routing.DefaultRoutePlanner;
import org.apache.hc.client5.http.ssl.ClientTlsStrategyBuilder;
import org.apache.hc.client5.http.ssl.HostnameVerificationPolicy;

public final class DelhiveryApacheHttpClientFactory {

    private DelhiveryApacheHttpClientFactory() {
    }

    public static CloseableHttpClient create() {
        return create(new DelhiverySecureDnsResolver());
    }

    public static CloseableHttpClient create(DnsResolver resolver) {
        Objects.requireNonNull(resolver, "DNS resolver required");

        var connectionManager =
            PoolingHttpClientConnectionManagerBuilder.create()
                .setDnsResolver(resolver)
                .setTlsSocketStrategy(
                    ClientTlsStrategyBuilder.create()
                        .setHostVerificationPolicy(
                            HostnameVerificationPolicy.BOTH
                        )
                        .buildClassic()
                )
                .setDefaultConnectionConfig(
                    ConnectionConfig.custom()
                        .setConnectTimeout(Timeout.ofSeconds(5))
                        .build()
                )
                .setMaxConnTotal(4)
                .setMaxConnPerRoute(2)
                .build();

        HttpRoutePlanner directRoutes =
            new DefaultRoutePlanner(null);

        var requestConfig = RequestConfig.custom()
            .setConnectionRequestTimeout(Timeout.ofSeconds(3))
            .setResponseTimeout(Timeout.ofSeconds(20))
            .build();

        return HttpClients.custom()
            .setConnectionManager(connectionManager)
            .setDefaultRequestConfig(requestConfig)
            .setRoutePlanner(directRoutes)
            .disableRedirectHandling()
            .disableAutomaticRetries()
            .disableCookieManagement()
            .disableAuthCaching()
            .disableConnectionState()
            .disableContentCompression()
            .build();
    }
}