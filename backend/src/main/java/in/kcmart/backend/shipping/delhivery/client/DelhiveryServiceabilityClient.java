package in.kcmart.backend.shipping.delhivery.client;

import in.kcmart.backend.shipping.delhivery.config.DelhiveryProperties;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class DelhiveryServiceabilityClient {

    private final RestClient restClient;
    private final DelhiveryProperties properties;

    public DelhiveryServiceabilityClient(
            RestClient.Builder builder,
            DelhiveryProperties properties) {

        this.properties = properties;

        SimpleClientHttpRequestFactory factory =
            new SimpleClientHttpRequestFactory();

        factory.setConnectTimeout(5000);
        factory.setReadTimeout(15000);

        this.restClient = builder
            .baseUrl(properties.getBaseUrl())
            .requestFactory(factory)
            .build();
    }

    public String fetchPincode(String pincode) {

        if (!properties.isEnabled()) {
            throw new IllegalStateException(
                "Delhivery integration is disabled."
            );
        }

        if (pincode == null ||
                !pincode.matches("[1-9][0-9]{5}")) {
            throw new IllegalArgumentException(
                "Invalid destination pincode."
            );
        }

        String token = properties.getApiToken();

        if (token == null || token.isBlank()) {
            throw new IllegalStateException(
                "Delhivery API token is not configured."
            );
        }

        return restClient.get()
            .uri(uriBuilder -> uriBuilder
                .path("/c/api/pin-codes/json/")
                .queryParam("filter_codes", pincode)
                .build())
            .header(
                HttpHeaders.AUTHORIZATION,
                "Token " + token
            )
            .retrieve()
            .body(String.class);
    }
}