package in.kcmart.backend.payment.razorpay.security;

import in.kcmart.backend.payment.razorpay.config.RazorpayProperties;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

@Component
public class RazorpaySignatureVerifier {

    private final RazorpayProperties properties;

    public RazorpaySignatureVerifier(
            RazorpayProperties properties) {

        this.properties = properties;
    }

    public boolean verifyPaymentSignature(
            String trustedRazorpayOrderId,
            String razorpayPaymentId,
            String razorpaySignature) {

        if (trustedRazorpayOrderId == null
                || trustedRazorpayOrderId.isBlank()
                || razorpayPaymentId == null
                || razorpayPaymentId.isBlank()
                || razorpaySignature == null
                || razorpaySignature.isBlank()) {

            return false;
        }

        if (!properties.credentialsConfigured()) {
            throw new IllegalStateException(
                "Razorpay credentials are not configured."
            );
        }

        try {
            String payload =
                trustedRazorpayOrderId
                    + "|"
                    + razorpayPaymentId;

            Mac mac = Mac.getInstance("HmacSHA256");

            SecretKeySpec secretKey =
                new SecretKeySpec(
                    properties.keySecret()
                        .getBytes(StandardCharsets.UTF_8),
                    "HmacSHA256"
                );

            mac.init(secretKey);

            byte[] expected =
                mac.doFinal(
                    payload.getBytes(StandardCharsets.UTF_8)
                );

            byte[] supplied;

            try {
                supplied =
                    HexFormat.of()
                        .parseHex(
                            razorpaySignature.trim()
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
                "Unable to verify Razorpay payment signature.",
                ex
            );
        }
    }
}