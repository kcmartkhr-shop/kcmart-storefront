package in.kcmart.backend.shipping.delhivery.client;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.HttpEntity;

public final class DelhiveryApacheResponseExtractor {

    public enum Kind {
        JSON,
        PDF
    }

    private DelhiveryApacheResponseExtractor() {
    }

    public static byte[] extract(
            ClassicHttpResponse response,
            Kind kind) throws IOException {

        Objects.requireNonNull(response, "Response required");
        Objects.requireNonNull(kind, "Response kind required");

        try (ClassicHttpResponse ownedResponse = response) {
            HttpEntity entity = ownedResponse.getEntity();

            if (entity == null) {
                throw new IOException("Missing Apache response entity.");
            }

            long length = entity.getContentLength();

            if (length < -1) {
                throw new IOException("Invalid Apache content length.");
            }

            try (InputStream input = entity.getContent()) {
                if (input == null) {
                    throw new IOException("Missing Apache response body.");
                }

                if (kind == Kind.JSON) {
                    return DelhiveryApacheBoundedResponseReader.readJson(
                        ownedResponse.getCode(),
                        length,
                        input
                    );
                }

                return DelhiveryApacheBoundedResponseReader.readPdf(
                    ownedResponse.getCode(),
                    length,
                    input
                );
            }
        }
    }
}