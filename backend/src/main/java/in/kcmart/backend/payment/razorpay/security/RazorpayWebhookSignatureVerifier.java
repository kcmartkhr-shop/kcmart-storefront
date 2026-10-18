package in.kcmart.backend.payment.razorpay.security;

import in.kcmart.backend.payment.razorpay.config.RazorpayProperties;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

@Component
public class RazorpayWebhookSignatureVerifier {

    private final RazorpayProperties properties;

    public RazorpayWebhookSignatureVerifier(
            RazorpayProperties properties) {
        this.properties = properties;
    }

    public boolean verify(
            byte[] rawBody,
            String suppliedSignature) {

        if (rawBody == null
                || suppliedSignature == null
                || suppliedSignature.isBlank()) {
            return false;
        }

        String secret = properties.webhookSecret();

        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException(
                    "Razorpay webhook secret is not configured."
            );
        }

        try {
            Mac mac = Mac.getInstance("HmacSHA256");

            mac.init(
                    new SecretKeySpec(
                            secret.getBytes(StandardCharsets.UTF_8),
                            "HmacSHA256"
                    )
            );

            byte[] expected = mac.doFinal(rawBody);

            byte[] supplied;

            try {
                supplied = HexFormat.of().parseHex(
                        suppliedSignature.trim()
                );
            } catch (IllegalArgumentException ex) {
                return false;
            }

            return MessageDigest.isEqual(
                    expected,
                    supplied
            );

        } catch (Exception ex) {
            throw new IllegalStateException(
                    "Unable to verify Razorpay webhook signature.",
                    ex
            );
        }
    }
}