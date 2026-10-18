package in.kcmart.backend.payment.razorpay.security;

import in.kcmart.backend.payment.razorpay.config.RazorpayProperties;
import org.junit.jupiter.api.Test;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RazorpayWebhookSignatureVerifierTest {

    private static final String SECRET = "unit-test-webhook-secret";

    private static final byte[] BODY =
            "{\"event\":\"payment.captured\"}"
                    .getBytes(StandardCharsets.UTF_8);

    private RazorpayWebhookSignatureVerifier verifier(String secret) {
        RazorpayProperties properties = mock(RazorpayProperties.class);
        when(properties.webhookSecret()).thenReturn(secret);
        return new RazorpayWebhookSignatureVerifier(properties);
    }

    private String signature(byte[] body, String secret) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(
                secret.getBytes(StandardCharsets.UTF_8),
                "HmacSHA256"
        ));
        return HexFormat.of().formatHex(mac.doFinal(body));
    }

    @Test
    void acceptsCorrectSignature() throws Exception {
        assertTrue(verifier(SECRET).verify(BODY, signature(BODY, SECRET)));
    }

    @Test
    void rejectsIncorrectSignature() {
        assertFalse(verifier(SECRET).verify(BODY, "00".repeat(32)));
    }

    @Test
    void rejectsSignatureForDifferentBody() throws Exception {
        String validSignature = signature(BODY, SECRET);
        byte[] modifiedBody =
                "{\"event\":\"payment.failed\"}"
                        .getBytes(StandardCharsets.UTF_8);

        assertFalse(verifier(SECRET).verify(modifiedBody, validSignature));
    }

    @Test
    void rejectsNullBody() {
        assertFalse(verifier(SECRET).verify(null, "abcd"));
    }

    @Test
    void rejectsNullSignature() {
        assertFalse(verifier(SECRET).verify(BODY, null));
    }

    @Test
    void rejectsBlankSignature() {
        assertFalse(verifier(SECRET).verify(BODY, "   "));
    }

    @Test
    void rejectsMalformedHexSignature() {
        assertFalse(verifier(SECRET).verify(BODY, "not-hex"));
    }

    @Test
    void rejectsSignatureWithWrongLength() {
        assertFalse(verifier(SECRET).verify(BODY, "abcd"));
    }

    @Test
    void rejectsWhenWebhookSecretIsMissing() {
        assertThrows(
                IllegalStateException.class,
                () -> verifier(null).verify(BODY, "abcd")
        );
    }

    @Test
    void rejectsWhenWebhookSecretIsBlank() {
        assertThrows(
                IllegalStateException.class,
                () -> verifier("   ").verify(BODY, "abcd")
        );
    }
}
