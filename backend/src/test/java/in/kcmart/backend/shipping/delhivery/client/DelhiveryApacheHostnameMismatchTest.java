package in.kcmart.backend.shipping.delhivery.client;

import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsServer;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.ssl.ClientTlsStrategyBuilder;
import org.apache.hc.client5.http.ssl.HostnameVerificationPolicy;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.core5.util.Timeout;
import org.junit.jupiter.api.Test;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class DelhiveryApacheHostnameMismatchTest {

    @Test
    void rejectsTrustedCertificateWithWrongHostname()
            throws Exception {

        Path directory = Files.createTempDirectory(
            "kcmart-apache-hostname-"
        );

        Path keyStorePath = directory.resolve("test.p12");
        String password = "local-test-only-123";
        HttpsServer server = null;

        try {
            String executable = System.getProperty("os.name")
                .toLowerCase().contains("win")
                    ? "keytool.exe" : "keytool";

            String keytool = Path.of(
                System.getProperty("java.home"),
                "bin",
                executable
            ).toString();

            Process process = new ProcessBuilder(
                keytool,
                "-genkeypair",
                "-alias", "wrong-host",
                "-keyalg", "RSA",
                "-keysize", "2048",
                "-validity", "1",
                "-storetype", "PKCS12",
                "-keystore", keyStorePath.toString(),
                "-storepass", password,
                "-keypass", password,
                "-dname", "CN=wrong-host.example",
                "-ext", "SAN=dns:wrong-host.example",
                "-noprompt"
            )
                .redirectErrorStream(true)
                .start();

            if (!process.waitFor(20, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                fail("keytool timed out");
            }

            assertEquals(0, process.exitValue());

            KeyStore store = KeyStore.getInstance("PKCS12");

            try (var input = Files.newInputStream(keyStorePath)) {
                store.load(input, password.toCharArray());
            }

            KeyManagerFactory kmf = KeyManagerFactory.getInstance(
                KeyManagerFactory.getDefaultAlgorithm()
            );
            kmf.init(store, password.toCharArray());

            TrustManagerFactory tmf = TrustManagerFactory.getInstance(
                TrustManagerFactory.getDefaultAlgorithm()
            );
            tmf.init(store);

            SSLContext serverContext = SSLContext.getInstance("TLS");
            serverContext.init(kmf.getKeyManagers(), null, null);

            SSLContext clientContext = SSLContext.getInstance("TLS");
            clientContext.init(null, tmf.getTrustManagers(), null);

            server = HttpsServer.create(
                new InetSocketAddress(
                    InetAddress.getByName("127.0.0.1"), 0
                ), 0
            );

            server.setHttpsConfigurator(
                new HttpsConfigurator(serverContext)
            );

            server.createContext("/", exchange -> {
                byte[] bytes = "TEST ONLY".getBytes(
                    java.nio.charset.StandardCharsets.UTF_8
                );
                exchange.sendResponseHeaders(200, bytes.length);
                try (var output = exchange.getResponseBody()) {
                    output.write(bytes);
                }
            });

            server.start();

            var manager =
                PoolingHttpClientConnectionManagerBuilder.create()
                    .setTlsSocketStrategy(
                        ClientTlsStrategyBuilder.create()
                            .setSslContext(clientContext)
                            .setHostVerificationPolicy(
                                HostnameVerificationPolicy.BOTH
                            )
                            .buildClassic()
                    )
                    .build();

            var config = RequestConfig.custom()
                .setConnectionRequestTimeout(Timeout.ofSeconds(3))
                .setResponseTimeout(Timeout.ofSeconds(5))
                .build();

            try (var client = HttpClients.custom()
                    .setConnectionManager(manager)
                    .setDefaultRequestConfig(config)
                    .disableRedirectHandling()
                    .disableAutomaticRetries()
                    .build()) {

                var request = new HttpGet(
                    "https://127.0.0.1:"
                        + server.getAddress().getPort() + "/"
                );

                Exception failure = assertThrows(
                    Exception.class,
                    () -> client.execute(request, response -> {
                        fail("Hostname mismatch must not succeed");
                        return null;
                    })
                );

                boolean tlsFailure = false;
                Throwable current = failure;

                while (current != null) {
                    if (current instanceof javax.net.ssl.SSLException) {
                        tlsFailure = true;
                        break;
                    }
                    current = current.getCause();
                }

                assertTrue(
                    tlsFailure,
                    "Expected TLS hostname rejection: " + failure
                );
            }
        }
        finally {
            if (server != null) {
                server.stop(0);
            }
            Files.deleteIfExists(keyStorePath);
            Files.deleteIfExists(directory);
        }
    }
}