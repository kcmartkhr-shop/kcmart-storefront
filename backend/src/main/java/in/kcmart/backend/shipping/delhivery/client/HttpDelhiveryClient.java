package in.kcmart.backend.shipping.delhivery.client;

import in.kcmart.backend.shipping.delhivery.config.DelhiveryProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.http.client.SimpleClientHttpRequestFactory;

@Component
@ConditionalOnProperty(
    name = "delhivery.enabled",
    havingValue = "true"
)
public class HttpDelhiveryClient implements DelhiveryClient {

    private final RestClient restClient;
    private final DelhiveryProperties properties;

    public HttpDelhiveryClient(
            RestClient.Builder builder,
            DelhiveryProperties properties) {

        this.properties = properties;
        SimpleClientHttpRequestFactory requestFactory =
            new SimpleClientHttpRequestFactory();

        requestFactory.setConnectTimeout(5000);
        requestFactory.setReadTimeout(15000);

        this.restClient = builder
            .baseUrl(properties.getBaseUrl())
            .requestFactory(requestFactory)
            .build();
    }

    @Override
    public String createShipment(String requestBody) {

        if (!properties.isShipmentCreationEnabled()) {
            throw new IllegalStateException(
                "Delhivery shipment creation is disabled."
            );
        }

        if (requestBody == null || requestBody.isBlank()) {
            throw new IllegalArgumentException(
                "Delhivery shipment request is required."
            );
        }

        String token = properties.getApiToken();

        if (token == null || token.isBlank()) {
            throw new IllegalStateException(
                "Delhivery API token is not configured."
            );
        }

        return restClient.post()
            .uri("/api/cmu/create.json")
            .header(
                HttpHeaders.AUTHORIZATION,
                "Token " + token
            )
            .contentType(
                org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED
            )
            .body("format=json&data=" +
                java.net.URLEncoder.encode(
                    requestBody,
                    java.nio.charset.StandardCharsets.UTF_8
                )
            )
            .retrieve()
            .body(String.class);
    }

    @Override
    public String trackShipment(String waybill) {

        if (waybill == null ||
                !waybill.matches("[A-Za-z0-9-]{5,50}")) {
            throw new IllegalArgumentException(
                "Invalid Delhivery waybill."
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
                .path("/api/v1/packages/json/")
                .queryParam("waybill", waybill)
                .build())
            .header(
                HttpHeaders.AUTHORIZATION,
                "Token " + token
            )
            .retrieve()
            .body(String.class);
    }
}