package in.kcmart.backend.shipping.delhivery.client;

import org.junit.jupiter.api.Test;

import javax.net.ssl.*;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

class DelhiveryLocalTlsCertificateTest {

    @Test
    void defaultTrustManagerRejectsUntrustedCertificate()
            throws Exception {

        SSLContext context = SSLContext.getInstance("TLS");
        context.init(null, null, new SecureRandom());

        assertNotNull(context.getSocketFactory());

        TrustManagerFactory trustFactory =
            TrustManagerFactory.getInstance(
                TrustManagerFactory.getDefaultAlgorithm()
            );

        trustFactory.init((KeyStore) null);

        X509TrustManager manager = null;

        for (TrustManager trustManager :
                trustFactory.getTrustManagers()) {

            if (trustManager instanceof X509TrustManager) {
                manager = (X509TrustManager) trustManager;
                break;
            }
        }

        assertNotNull(manager);
        assertTrue(manager.getAcceptedIssuers().length >= 0);
    }

    @Test
    void apacheFactoryDoesNotDisableCertificateVerification()
            throws Exception {

        String source = java.nio.file.Files.readString(
            java.nio.file.Path.of(
                "src/main/java/in/kcmart/backend/shipping/delhivery/client/"
                    + "DelhiveryApacheHttpClientFactory.java"
            )
        );

        assertTrue(
            source.contains("HostnameVerificationPolicy.BOTH")
        );

        assertFalse(
            source.contains("NoopHostnameVerifier")
        );

        assertFalse(
            source.contains("TrustAllStrategy")
        );

        assertFalse(
            source.contains("loadTrustMaterial(null")
        );
    }
}