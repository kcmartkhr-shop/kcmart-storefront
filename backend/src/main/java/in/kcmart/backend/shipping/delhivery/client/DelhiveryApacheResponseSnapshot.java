package in.kcmart.backend.shipping.delhivery.client;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;

import org.apache.hc.core5.http.ClassicHttpResponse;

public final class DelhiveryApacheResponseSnapshot {

    private final int status;
    private final byte[] body;
    private final Map<String, String> headers;

    private DelhiveryApacheResponseSnapshot(
            int status,
            byte[] body,
            Map<String, String> headers) {

        this.status = status;
        this.body = body.clone();
        this.headers = Map.copyOf(headers);
    }

    public static DelhiveryApacheResponseSnapshot capture(
            ClassicHttpResponse response,
            DelhiveryApacheResponseExtractor.Kind kind)
            throws IOException {

        Objects.requireNonNull(response, "Response required");
        Objects.requireNonNull(kind, "Response kind required");

        int status = response.getCode();

        String contentType = null;

        var header = response.getFirstHeader("Content-Type");

        if (header != null) {
            contentType = header.getValue();
        }

        byte[] bytes = DelhiveryApacheResponseExtractor.extract(
            response,
            kind
        );

        Map<String, String> safeHeaders =
            contentType == null || contentType.isBlank()
                ? Map.of()
                : Map.of("content-type", contentType);

        return new DelhiveryApacheResponseSnapshot(
            status,
            bytes,
            safeHeaders
        );
    }

    public int statusCode() {
        return status;
    }

    public byte[] bodyBytes() {
        return body.clone();
    }

    public InputStream openBodyStream() {
        return new ByteArrayInputStream(body);
    }

    public Map<String, String> safeHeaders() {
        return headers;
    }

    public int bodySize() {
        return body.length;
    }
}