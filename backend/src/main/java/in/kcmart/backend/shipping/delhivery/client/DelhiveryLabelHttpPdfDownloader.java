package in.kcmart.backend.shipping.delhivery.client;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public final class DelhiveryLabelHttpPdfDownloader {

    private static final int MAX_BYTES = 5 * 1024 * 1024;

    @FunctionalInterface
    public interface Transport {
        HttpResponse<InputStream> send(
            HttpRequest request
        ) throws IOException, InterruptedException;
    }

    private final Transport transport;

    public DelhiveryLabelHttpPdfDownloader() {
        this.transport = request -> {
            throw new IOException(
                "Delhivery network transport is disabled until "
                + "DNS-pinned HTTPS transport is verified."
            );
        };
    }
    public DelhiveryLabelHttpPdfDownloader(Transport transport) {
        this.transport = java.util.Objects.requireNonNull(
            transport
        );
    }

    public byte[] download(String url)
            throws IOException, InterruptedException {

        URI uri = DelhiveryLabelPdfUrlValidator.validate(url);

        HttpRequest request = HttpRequest.newBuilder(uri)
            .timeout(Duration.ofSeconds(20))
            .header("Accept", "application/pdf")
            .GET()
            .build();

        HttpResponse<InputStream> response =
            transport.send(request);

        if (response == null) {
            throw new IOException("Missing PDF response.");
        }

        int status = response.statusCode();

        if (status != 200) {
            try (InputStream body = response.body()) {
                // Close rejected response body.
            }

            throw new IOException(
                "Unexpected PDF response status: " + status
            );
        }

        long length = response.headers()
            .firstValueAsLong("Content-Length")
            .orElse(-1);

        if (length > MAX_BYTES) {
            try (InputStream body = response.body()) {
                // Close oversized response body.
            }

            throw new IOException(
                "PDF response exceeds maximum permitted size."
            );
        }

        return DelhiveryLabelPdfResponseReader.read(
            response.body()
        );
    }
}