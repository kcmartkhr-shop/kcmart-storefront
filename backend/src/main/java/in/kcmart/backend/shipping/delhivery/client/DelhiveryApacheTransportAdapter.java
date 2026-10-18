package in.kcmart.backend.shipping.delhivery.client;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

public final class DelhiveryApacheTransportAdapter {

    private static final Set<String> ALLOWED_HOSTS = Set.of(
        "track.delhivery.com",
        "staging-express.delhivery.com",
        "s3.amazonaws.com",
        "s3.ap-south-1.amazonaws.com"
    );

    private DelhiveryApacheTransportAdapter() {
    }

    public static URI validateRequest(HttpRequest request)
            throws IOException {

        if (request == null) {
            throw new IOException("HTTP request is required.");
        }

        if (!"GET".equals(request.method())) {
            throw new IOException("Only GET requests are permitted.");
        }

        if (request.bodyPublisher().isPresent()) {
            throw new IOException("Request body is not permitted.");
        }

        URI uri = Objects.requireNonNull(
            request.uri(),
            "Request URI is required."
        );

        if (!"https".equalsIgnoreCase(uri.getScheme())) {
            throw new IOException("HTTPS is required.");
        }

        if (uri.getUserInfo() != null ||
                uri.getFragment() != null ||
                uri.getPort() != -1) {
            throw new IOException("Unsupported URI components.");
        }

        String host = uri.getHost();

        if (host == null ||
                !ALLOWED_HOSTS.contains(
                    host.toLowerCase(Locale.ROOT))) {
            throw new IOException("Destination host is not allowed.");
        }

        if (uri.getRawPath() == null ||
                uri.getRawPath().isBlank() ||
                "/".equals(uri.getRawPath())) {
            throw new IOException("Request path is required.");
        }

        validateHeaders(request, host.toLowerCase(Locale.ROOT));

        return uri;
    }

    private static void validateHeaders(
            HttpRequest request,
            String host) throws IOException {

        boolean apiHost =
            "track.delhivery.com".equals(host) ||
            "staging-express.delhivery.com".equals(host);

        for (var entry : request.headers().map().entrySet()) {
            String name = entry.getKey().toLowerCase(Locale.ROOT);
            var values = entry.getValue();

            if (values == null || values.size() != 1) {
                throw new IOException("Invalid request header values.");
            }

            String value = values.get(0);

            if (value == null || value.isBlank() ||
                    value.indexOf('\r') >= 0 ||
                    value.indexOf('\n') >= 0) {
                throw new IOException("Invalid request header value.");
            }

            if ("accept".equals(name)) {
                String expected = apiHost
                    ? "application/json"
                    : "application/pdf";

                if (!expected.equals(value)) {
                    throw new IOException("Unsupported Accept header.");
                }
                continue;
            }

            if ("authorization".equals(name) && apiHost) {
                if (!value.startsWith("Token ") ||
                        value.length() <= "Token ".length() ||
                        !value.substring("Token ".length())
                            .matches("[A-Za-z0-9._~-]{1,512}")) {
                    throw new IOException(
                        "Invalid Delhivery authorization format."
                    );
                }
                continue;
            }

            throw new IOException(
                "Request contains a disallowed header."
            );
        }

        String expectedAccept = apiHost
            ? "application/json"
            : "application/pdf";

        if (!request.headers()
                .firstValue("Accept")
                .filter(expectedAccept::equals)
                .isPresent()) {
            throw new IOException("Required Accept header missing.");
        }

        if (apiHost &&
                request.headers().firstValue("Authorization").isEmpty()) {
            throw new IOException(
                "Delhivery API authorization header missing."
            );
        }
    }

    public static void sendDisabled(HttpRequest request)
            throws IOException {

        validateRequest(request);

        throw new IOException(
            "Apache transport adapter execution remains disabled."
        );
    }
}