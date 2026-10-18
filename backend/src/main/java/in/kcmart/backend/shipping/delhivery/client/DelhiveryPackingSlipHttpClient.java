package in.kcmart.backend.shipping.delhivery.client;

import java.io.IOException;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Objects;

public final class DelhiveryPackingSlipHttpClient {

    private static final int MAX_JSON_BYTES = 256 * 1024;

    @FunctionalInterface
    public interface Transport {
        HttpResponse<InputStream> send(HttpRequest request)
            throws IOException, InterruptedException;
    }

    private final Transport transport;

    public DelhiveryPackingSlipHttpClient() {
        this.transport = request -> {
            throw new IOException(
                "Delhivery network transport is disabled until "
                + "DNS-pinned HTTPS transport is verified."
            );
        };
    }
    public DelhiveryPackingSlipHttpClient(Transport transport) {
        this.transport = Objects.requireNonNull(transport);
    }

    public String fetchJson(
            String baseUrl,
            String apiToken,
            String waybill)
            throws IOException, InterruptedException {

        HttpRequest request =
            DelhiveryPackingSlipRequestFactory.create(
                baseUrl, apiToken, waybill
            );

        HttpResponse<InputStream> response =
            transport.send(request);

        if (response == null) {
            throw new IOException("Missing packing slip response.");
        }

        try (InputStream input = response.body()) {

            if (input == null) {
                throw new IOException("Missing response body.");
            }

            if (response.statusCode() != 200) {
                throw new IOException(
                    "Unexpected packing slip HTTP status: " +
                    response.statusCode()
                );
            }

            long declaredLength = response.headers()
                .firstValueAsLong("Content-Length")
                .orElse(-1);

            if (declaredLength > MAX_JSON_BYTES) {
                throw new IOException(
                    "Packing slip JSON exceeds size limit."
                );
            }

            ByteArrayOutputStream output =
                new ByteArrayOutputStream();

            byte[] buffer = new byte[4096];
            int count;
            int total = 0;

            while ((count = input.read(buffer)) != -1) {
                if (count == 0) {
                    continue;
                }

                if (count > MAX_JSON_BYTES - total) {
                    throw new IOException(
                        "Packing slip JSON exceeds size limit."
                    );
                }

                output.write(buffer, 0, count);
                total += count;
            }

            if (total == 0) {
                throw new IOException(
                    "Empty packing slip JSON response."
                );
            }

            return output.toString(StandardCharsets.UTF_8);
        }
    }
}