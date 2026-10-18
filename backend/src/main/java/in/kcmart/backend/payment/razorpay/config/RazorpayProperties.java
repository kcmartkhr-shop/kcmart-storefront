package in.kcmart.backend.payment.razorpay.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "kcmart.razorpay")
public record RazorpayProperties(
        String keyId,
        String keySecret,
        String webhookSecret,
        String baseUrl) {

    public boolean credentialsConfigured() {
        return hasText(keyId) && hasText(keySecret);
    }

    public boolean webhookConfigured() {
        return hasText(webhookSecret);
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}