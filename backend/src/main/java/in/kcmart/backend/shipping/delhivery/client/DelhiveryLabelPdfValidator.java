package in.kcmart.backend.shipping.delhivery.client;

import java.nio.charset.StandardCharsets;

public final class DelhiveryLabelPdfValidator {

    public static final int MAX_PDF_BYTES = 5 * 1024 * 1024;

    private static final byte[] HEADER =
        "%PDF-".getBytes(StandardCharsets.US_ASCII);

    private static final byte[] EOF =
        "%%EOF".getBytes(StandardCharsets.US_ASCII);

    private DelhiveryLabelPdfValidator() {
    }

    public static byte[] validate(byte[] pdf) {

        if (pdf == null || pdf.length == 0) {
            throw new IllegalArgumentException(
                "Delhivery label PDF response is empty."
            );
        }

        if (pdf.length > MAX_PDF_BYTES) {
            throw new IllegalArgumentException(
                "Delhivery label PDF exceeds maximum size."
            );
        }

        if (!startsWith(pdf, HEADER)) {
            throw new IllegalArgumentException(
                "Delhivery label response is not a PDF."
            );
        }

        if (!hasEofMarker(pdf)) {
            throw new IllegalArgumentException(
                "Delhivery label PDF is incomplete."
            );
        }

        return pdf.clone();
    }

    private static boolean startsWith(byte[] data, byte[] prefix) {

        if (data.length < prefix.length) {
            return false;
        }

        for (int i = 0; i < prefix.length; i++) {
            if (data[i] != prefix[i]) {
                return false;
            }
        }

        return true;
    }

    private static boolean hasEofMarker(byte[] data) {

        int start = Math.max(0, data.length - 1024);

        for (int i = data.length - EOF.length; i >= start; i--) {

            boolean match = true;

            for (int j = 0; j < EOF.length; j++) {
                if (data[i + j] != EOF[j]) {
                    match = false;
                    break;
                }
            }

            if (match) {
                return true;
            }
        }

        return false;
    }
}