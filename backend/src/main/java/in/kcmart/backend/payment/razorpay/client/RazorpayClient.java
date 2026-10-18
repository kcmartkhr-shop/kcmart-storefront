package in.kcmart.backend.payment.razorpay.client;

import in.kcmart.backend.payment.razorpay.config.RazorpayProperties;
import org.springframework.http.MediaType;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class RazorpayClient {

    private final RazorpayProperties properties;
    private final RestClient restClient;

    public RazorpayClient(RazorpayProperties properties) {
        this.properties = properties;

        String baseUrl = properties.baseUrl();

        if (baseUrl == null || baseUrl.isBlank()) {
            baseUrl = "https://api.razorpay.com";
        }

        var httpClient = HttpClients.custom()
                .disableAutomaticRetries()
                .build();

        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(
                        new HttpComponentsClientHttpRequestFactory(httpClient)
                )
                .build();
    }

    public RazorpayOrderResponse createOrder(
            long amountPaise,
            String receipt,
            Map<String, String> notes) {

        requireCredentials();

        if (amountPaise <= 0) {
            throw new IllegalArgumentException(
                    "Razorpay order amount must be greater than zero."
            );
        }

        if (receipt == null || receipt.isBlank()) {
            throw new IllegalArgumentException(
                    "Razorpay receipt is required."
            );
        }

        if (receipt.length() > 40) {
            throw new IllegalArgumentException(
                    "Razorpay receipt must not exceed 40 characters."
            );
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("amount", amountPaise);
        body.put("currency", "INR");
        body.put("receipt", receipt);
        body.put("partial_payment", false);

        if (notes != null && !notes.isEmpty()) {
            body.put("notes", notes);
        }

        RazorpayOrderResponse response = restClient.post()
                .uri("/v1/orders")
                .headers(headers ->
                        headers.setBasicAuth(
                                properties.keyId(),
                                properties.keySecret()
                        )
                )
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(RazorpayOrderResponse.class);

        if (response == null ||
                response.id() == null ||
                response.id().isBlank()) {

            throw new IllegalStateException(
                    "Razorpay returned an invalid order response."
            );
        }

        return response;
    }

    public RazorpayPaymentResponse fetchPayment(
            String paymentId) {

        requireCredentials();

        if (paymentId == null || paymentId.isBlank()) {
            throw new IllegalArgumentException(
                    "Razorpay payment ID is required."
            );
        }

        String safePaymentId = paymentId.trim();

        if (safePaymentId.length() > 100) {
            throw new IllegalArgumentException(
                    "Razorpay payment ID is invalid."
            );
        }

        RazorpayPaymentResponse response =
                restClient.get()
                    .uri(
                        "/v1/payments/{paymentId}",
                        safePaymentId
                    )
                    .headers(headers ->
                        headers.setBasicAuth(
                            properties.keyId(),
                            properties.keySecret()
                        )
                    )
                    .retrieve()
                    .body(RazorpayPaymentResponse.class);

        if (response == null
                || response.id() == null
                || response.id().isBlank()) {

            throw new IllegalStateException(
                    "Razorpay returned an invalid payment response."
            );
        }

        return response;
    }
    /**
     * READ-ONLY gateway lookup.
     * Does not create, retry, or initiate a refund.
     */
    public RazorpayRefundResponse fetchRefund(String refundId) {
        requireCredentials();

        if (refundId == null
                || refundId.isBlank()
                || !refundId.equals(refundId.trim())
                || refundId.length() > 100
                || !refundId.matches("rfnd_[A-Za-z0-9_]+")) {
            throw new IllegalArgumentException(
                "Razorpay refund ID is invalid."
            );
        }

        RazorpayRefundResponse response = restClient.get()
                .uri("/v1/refunds/{refundId}", refundId)
                .headers(headers ->
                        headers.setBasicAuth(
                                properties.keyId(),
                                properties.keySecret()
                        )
                )
                .retrieve()
                .body(RazorpayRefundResponse.class);

        if (response == null
                || !refundId.equals(response.id())
                || response.payment_id() == null
                || response.payment_id().isBlank()
                || response.amount() == null
                || response.amount() <= 0
                || response.currency() == null
                || response.currency().isBlank()
                || response.status() == null
                || response.status().isBlank()) {

            throw new IllegalStateException(
                "Razorpay returned an invalid refund response."
            );
        }

        return response;
    }

    public record RazorpayRefundResponse(
            String id,
            String entity,
            String payment_id,
            Long amount,
            String currency,
            String status,
            Long created_at) {
    }
    private void requireCredentials() {
        if (!properties.credentialsConfigured()) {
            throw new IllegalStateException(
                    "Razorpay credentials are not configured."
            );
        }
    }

    public record RazorpayPaymentResponse(
            String id,
            String entity,
            Long amount,
            String currency,
            String status,
            String order_id,
            String method,
            Boolean captured,
            String email,
            String contact,
            Long created_at) {
    }
    public record RazorpayOrderResponse(
            String id,
            String entity,
            Long amount,
            Long amount_paid,
            Long amount_due,
            String currency,
            String receipt,
            String status,
            Integer attempts,
            Long created_at) {
    }
}