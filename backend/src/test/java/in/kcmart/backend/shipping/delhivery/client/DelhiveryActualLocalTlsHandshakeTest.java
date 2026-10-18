package in.kcmart.backend.shipping.delhivery.client;

import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsServer;

import org.junit.jupiter.api.Test;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLHandshakeException;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

class DelhiveryActualLocalTlsHandshakeTest {

    @Test
    void rejectsSelfSignedCertificateDuringRealHandshake()
            throws Exception {

        Path directory = Files.createTempDirectory(
            "kcmart-local-tls-"
        );

        Path keyStorePath = directory.resolve("local-test.p12");

        String password = "local-test-only-123";

        HttpsServer server = null;

        try {
            String keytool = Path.of(
                System.getProperty("java.home"),
                "bin",
                "keytool.exe"
            ).toString();

            Process process = new ProcessBuilder(
                keytool,
                "-genkeypair",
                "-alias", "local-test",
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

            boolean finished = process.waitFor(
                20,
                java.util.concurrent.TimeUnit.SECONDS
            );

            if (!finished) {
                process.destroyForcibly();
                fail("Local keytool process timed out");
            }

            assertEquals(
                0,
                process.exitValue(),
                "Local keytool certificate generation failed"
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
                    InetAddress.getLoopbackAddress(),
                    0
                ),
                0
            );

            server.setHttpsConfigurator(
                new HttpsConfigurator(serverContext)
            );

            server.createContext("/", exchange -> {
                byte[] response = "test".getBytes(
                    java.nio.charset.StandardCharsets.UTF_8
                );

                exchange.sendResponseHeaders(200, response.length);

                try (var output = exchange.getResponseBody()) {
                    output.write(response);
                }
            });

            server.start();

            int port = server.getAddress().getPort();

            try (HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(3))
                    .build()) {

                HttpRequest request =
                    HttpRequest.newBuilder()
                        .uri(URI.create(
                            "https://localhost:" + port + "/"
                        ))
                        .timeout(Duration.ofSeconds(5))
                        .GET()
                        .build();

                Exception failure = assertThrows(
                    Exception.class,
                    () -> client.send(
                        request,
                        HttpResponse.BodyHandlers.ofString()
                    )
                );

                boolean handshakeFailure = false;

                Throwable current = failure;

                while (current != null) {
                    if (current instanceof SSLHandshakeException) {
                        handshakeFailure = true;
                        break;
                    }

                    current = current.getCause();
                }

                assertTrue(
                    handshakeFailure,
                    "Expected an SSL handshake rejection"
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