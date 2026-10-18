package in.kcmart.backend.shipping.delhivery.client;

import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsServer;

import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.ssl.ClientTlsStrategyBuilder;
import org.apache.hc.client5.http.ssl.HostnameVerificationPolicy;
import org.apache.hc.core5.util.Timeout;
import org.apache.hc.client5.http.config.RequestConfig;
import org.junit.jupiter.api.Test;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class DelhiveryApacheActualTlsHandshakeTest {

    @Test
    void apacheRejectsUntrustedSelfSignedCertificate()
            throws Exception {

        Path directory = Files.createTempDirectory(
            "kcmart-apache-tls-"
        );

        Path keyStorePath = directory.resolve("local-test.p12");
        String password = "local-test-only-123";

        HttpsServer server = null;

        try {
            String executable = System.getProperty("os.name")
                .toLowerCase().contains("win")
                    ? "keytool.exe"
                    : "keytool";

            String keytool = Path.of(
                System.getProperty("java.home"),
                "bin",
                executable
            ).toString();

            Process process = new ProcessBuilder(
                keytool,
                "-genkeypair",
                "-alias", "apache-local-test",
                "-keyalg", "RSA",
                "-keysize", "2048",
                "-validity", "1",
                "-storetype", "PKCS12",
                "-keystore", keyStorePath.toString(),
                "-storepass", password,
                "-keypass", password,
                "-dname", "CN=localhost",
                "-ext", "SAN=dns:localhost,ip:127.0.0.1",
                "-noprompt"
            )
                .redirectErrorStream(true)
                .start();

            if (!process.waitFor(20, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                fail("Local keytool process timed out");
            }

            assertEquals(
                0,
                process.exitValue(),
                "Local certificate generation failed"
            );

            KeyStore store = KeyStore.getInstance("PKCS12");

            try (var input = Files.newInputStream(keyStorePath)) {
                store.load(input, password.toCharArray());
            }

            KeyManagerFactory keyManagers =
                KeyManagerFactory.getInstance(
                    KeyManagerFactory.getDefaultAlgorithm()
                );

            keyManagers.init(store, password.toCharArray());

            SSLContext serverContext =
                SSLContext.getInstance("TLS");

            serverContext.init(
                keyManagers.getKeyManagers(),
                null,
                null
            );

            server = HttpsServer.create(
                new InetSocketAddress(
                    InetAddress.getByName("127.0.0.1"),
                    0
                ),
                0
            );

            server.setHttpsConfigurator(
                new HttpsConfigurator(serverContext)
            );

            server.createContext("/", exchange -> {
                byte[] bytes = "LOCAL TEST ONLY".getBytes(
                    java.nio.charset.StandardCharsets.UTF_8
                );

                exchange.sendResponseHeaders(200, bytes.length);

                try (var output = exchange.getResponseBody()) {
                    output.write(bytes);
                }
            });

            server.start();

            int port = server.getAddress().getPort();

            // Test-only connection manager. Production secure DNS
            // resolver is NOT changed or bypassed in production.
            var manager =
                PoolingHttpClientConnectionManagerBuilder.create()
                    .setTlsSocketStrategy(
                        ClientTlsStrategyBuilder.create()
                            .setHostVerificationPolicy(
                                HostnameVerificationPolicy.BOTH
                            )
                            .buildClassic()
                    )
                    .build();

            var config = RequestConfig.custom()
                .setConnectionRequestTimeout(
                    Timeout.ofSeconds(3)
                )
                .setResponseTimeout(
                    Timeout.ofSeconds(5)
                )
                .build();

            try (var client = HttpClients.custom()
                    .setConnectionManager(manager)
                    .setDefaultRequestConfig(config)
                    .disableRedirectHandling()
                    .disableAutomaticRetries()
                    .disableCookieManagement()
                    .build()) {

                var request = new HttpGet(
                    "https://127.0.0.1:" + port + "/"
                );

                Exception failure = assertThrows(
                    Exception.class,
                    () -> client.execute(request, response -> {
                        fail(
                            "Untrusted TLS server must not return HTTP response"
                        );
                        return null;
                    })
                );

                boolean certificateRelated = false;
                Throwable current = failure;

                while (current != null) {
                    String name = current.getClass().getName();

                    if (name.contains("SSLHandshakeException")
                            || name.contains("CertificateException")
                            || name.contains("SSLPeerUnverifiedException")) {
                        certificateRelated = true;
                        break;
                    }

                    current = current.getCause();
                }

                assertTrue(
                    certificateRelated,
                    "Expected Apache TLS certificate rejection; got: "
                        + failure
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