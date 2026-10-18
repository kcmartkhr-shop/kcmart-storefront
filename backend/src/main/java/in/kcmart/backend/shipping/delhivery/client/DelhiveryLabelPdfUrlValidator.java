package in.kcmart.backend.shipping.delhivery.client;

import java.net.URI;
import java.util.Locale;
import java.util.Set;

public final class DelhiveryLabelPdfUrlValidator {

    private static final Set<String> ALLOWED_HOSTS = Set.of(
        "s3.amazonaws.com",
        "s3.ap-south-1.amazonaws.com"
    );

    private DelhiveryLabelPdfUrlValidator() {
    }

    public static URI validate(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                "PDF URL is required."
            );
        }

        URI uri;

        try {
            uri = URI.create(value);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException(
                "Invalid PDF URL.", ex
            );
        }

        if (!"https".equalsIgnoreCase(uri.getScheme())) {
            throw new IllegalArgumentException(
                "HTTPS is required."
            );
        }

        if (uri.getUserInfo() != null ||
                uri.getFragment() != null ||
                uri.getPort() != -1) {
            throw new IllegalArgumentException(
                "Unsupported PDF URL components."
            );
        }

        String host = uri.getHost();

        if (host == null ||
                !ALLOWED_HOSTS.contains(
                    host.toLowerCase(Locale.ROOT))) {
            throw new IllegalArgumentException(
                "PDF URL host is not allowed."
            );
        }

        if (uri.getRawPath() == null ||
                uri.getRawPath().isBlank() ||
                "/".equals(uri.getRawPath())) {
            throw new IllegalArgumentException(
                "PDF URL path is required."
            );
        }

        return uri;
    }
}