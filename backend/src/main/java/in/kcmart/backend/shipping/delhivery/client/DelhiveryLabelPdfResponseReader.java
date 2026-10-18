package in.kcmart.backend.shipping.delhivery.client;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public final class DelhiveryLabelPdfResponseReader {

    private static final int MAX_BYTES = 5 * 1024 * 1024;

    private DelhiveryLabelPdfResponseReader() {
    }

    public static byte[] read(InputStream input) throws IOException {

        if (input == null) {
            throw new IllegalArgumentException(
                "PDF response stream required."
            );
        }

        try (InputStream stream = input;
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {

            byte[] buffer = new byte[8192];
            int total = 0;
            int count;

            while ((count = stream.read(buffer)) != -1) {
                if (count == 0) {
                    continue;
                }

                if (count > MAX_BYTES - total) {
                    throw new IllegalArgumentException(
                        "PDF exceeds maximum permitted size."
                    );
                }

                output.write(buffer, 0, count);
                total += count;
            }

            return DelhiveryLabelPdfValidator.validate(
                output.toByteArray()
            );
        }
    }
}