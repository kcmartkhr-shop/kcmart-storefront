package in.kcmart.backend.shipping.delhivery.client;

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import javax.net.ssl.SSLSession;

public final class DelhiveryJavaHttpResponseBridge {

    private DelhiveryJavaHttpResponseBridge() {
    }

    public static HttpResponse<InputStream> fromSnapshot(
            HttpRequest request,
            DelhiveryApacheResponseSnapshot snapshot) {

        Objects.requireNonNull(request, "Request required");
        Objects.requireNonNull(snapshot, "Snapshot required");

        return new SnapshotResponse(request, snapshot);
    }

    private static final class SnapshotResponse
            implements HttpResponse<InputStream> {

        private final HttpRequest request;
        private final int status;
        private final HttpHeaders headers;
        private final byte[] body;

        private SnapshotResponse(
                HttpRequest request,
                DelhiveryApacheResponseSnapshot snapshot) {

            this.request = request;
            this.status = snapshot.statusCode();
            this.body = snapshot.bodyBytes();

            Map<String, List<String>> values =
                snapshot.safeHeaders().entrySet()
                    .stream()
                    .collect(
                        java.util.stream.Collectors.toUnmodifiableMap(
                            Map.Entry::getKey,
                            entry -> List.of(entry.getValue())
                        )
                    );

            this.headers = HttpHeaders.of(
                values,
                (name, value) -> true
            );
        }

        @Override
        public int statusCode() {
            return status;
        }

        @Override
        public HttpRequest request() {
            return request;
        }

        @Override
        public Optional<HttpResponse<InputStream>> previousResponse() {
            return Optional.empty();
        }

        @Override
        public HttpHeaders headers() {
            return headers;
        }

        @Override
        public InputStream body() {
            return new java.io.ByteArrayInputStream(body);
        }

        @Override
        public Optional<SSLSession> sslSession() {
            return Optional.empty();
        }

        @Override
        public URI uri() {
            return request.uri();
        }

        @Override
        public HttpClient.Version version() {
            return HttpClient.Version.HTTP_1_1;
        }
    }
}