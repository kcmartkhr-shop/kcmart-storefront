package in.kcmart.backend.shipping.delhivery.client;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

public final class DelhiveryApacheBoundedResponseReader {

    public static final int MAX_JSON_BYTES = 256 * 1024;
    public static final int MAX_PDF_BYTES = 5 * 1024 * 1024;

    private DelhiveryApacheBoundedResponseReader() {
    }

    public static byte[] readJson(
            int status,
            long declaredLength,
            InputStream input) throws IOException {

        return read(status, declaredLength, input, MAX_JSON_BYTES);
    }

    public static byte[] readPdf(
            int status,
            long declaredLength,
            InputStream input) throws IOException {

        return read(status, declaredLength, input, MAX_PDF_BYTES);
    }

    private static byte[] read(
            int status,
            long declaredLength,
            InputStream input,
            int maxBytes) throws IOException {

        Objects.requireNonNull(input, "Response body required");

        try (InputStream body = input) {
            if (status != 200) {
                throw new IOException("Unexpected HTTP response status.");
            }

            if (declaredLength < -1 || declaredLength > maxBytes) {
                throw new IOException("Invalid response content length.");
            }

            ByteArrayOutputStream output = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int total = 0;
            int count;

            while ((count = body.read(buffer)) != -1) {
                if (count == 0) {
                    continue;
                }

                if (count > maxBytes - total) {
                    throw new IOException(
                        "Response exceeds maximum permitted size."
                    );
                }

                output.write(buffer, 0, count);
                total += count;
            }

            if (total == 0) {
                throw new IOException("Empty HTTP response body.");
            }

            if (declaredLength >= 0 && declaredLength != total) {
                throw new IOException(
                    "Response content length mismatch."
                );
            }

            return output.toByteArray();
        }
    }
}