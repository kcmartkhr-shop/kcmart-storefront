package in.kcmart.backend.shipping.delhivery.client;

import java.net.URI;
import java.net.http.HttpRequest;
import java.time.Duration;
import java.util.Objects;

public final class DelhiveryPackingSlipRequestFactory {

    private DelhiveryPackingSlipRequestFactory() {
    }

    public static HttpRequest create(
            String baseUrl,
            String apiToken,
            String waybill) {

        if (baseUrl == null ||
                !(baseUrl.equals("https://track.delhivery.com") ||
                  baseUrl.equals("https://staging-express.delhivery.com"))) {
            throw new IllegalArgumentException(
                "Unapproved Delhivery API base URL."
            );
        }

        if (apiToken == null ||
                apiToken.isBlank() ||
                apiToken.indexOf('\r') >= 0 ||
                apiToken.indexOf('\n') >= 0) {
            throw new IllegalArgumentException(
                "Valid Delhivery API token required."
            );
        }

        if (waybill == null ||
                !waybill.matches("[A-Za-z0-9-]{5,50}")) {
            throw new IllegalArgumentException(
                "Invalid Delhivery waybill."
            );
        }

        URI uri = URI.create(
            baseUrl +
            "/api/p/packing_slip?wbns=" +
            waybill +
            "&pdf=true&pdf_size=4R"
        );

        return HttpRequest.newBuilder(uri)
            .timeout(Duration.ofSeconds(20))
            .header("Accept", "application/json")
            .header("Authorization", "Token " + apiToken.trim())
            .GET()
            .build();
    }
}