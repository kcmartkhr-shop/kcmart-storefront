package in.kcmart.backend.shipping.delhivery.mock;

import in.kcmart.backend.shipping.delhivery.client.DelhiveryLabelPdfValidator;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

public final class MockShippingLabelPdfFactory {

    private MockShippingLabelPdfFactory() {
    }

    public static byte[] create(String waybill) {

        if (waybill == null ||
                !waybill.matches("[A-Za-z0-9-]{5,50}")) {
            throw new IllegalArgumentException(
                "Valid test waybill required."
            );
        }

        String message = "KCMART TEST LABEL - NOT FOR SHIPPING";
        String awbText = "TEST AWB: " + waybill;

        String stream = "BT\n"
            + "/F1 16 Tf\n"
            + "50 750 Td\n"
            + "(" + message + ") Tj\n"
            + "0 -35 Td\n"
            + "/F1 12 Tf\n"
            + "(" + awbText + ") Tj\n"
            + "0 -35 Td\n"
            + "(DEVELOPMENT ONLY - NOT A REAL DELHIVERY LABEL) Tj\n"
            + "ET\n";

        byte[] streamBytes =
            stream.getBytes(StandardCharsets.US_ASCII);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int[] offsets = new int[6];

        write(out, "%PDF-1.4\n");

        offsets[1] = out.size();
        write(out, "1 0 obj\n"
            + "<< /Type /Catalog /Pages 2 0 R >>\n"
            + "endobj\n");

        offsets[2] = out.size();
        write(out, "2 0 obj\n"
            + "<< /Type /Pages /Kids [3 0 R] /Count 1 >>\n"
            + "endobj\n");

        offsets[3] = out.size();
        write(out, "3 0 obj\n"
            + "<< /Type /Page /Parent 2 0 R "
            + "/MediaBox [0 0 612 792] "
            + "/Resources << /Font << /F1 4 0 R >> >> "
            + "/Contents 5 0 R >>\n"
            + "endobj\n");

        offsets[4] = out.size();
        write(out, "4 0 obj\n"
            + "<< /Type /Font /Subtype /Type1 "
            + "/BaseFont /Helvetica >>\n"
            + "endobj\n");

        offsets[5] = out.size();
        write(out, "5 0 obj\n"
            + "<< /Length " + streamBytes.length + " >>\n"
            + "stream\n");
        out.writeBytes(streamBytes);
        write(out, "endstream\nendobj\n");

        int xrefOffset = out.size();

        write(out, "xref\n0 6\n");
        write(out, "0000000000 65535 f \n");

        for (int i = 1; i <= 5; i++) {
            write(out, String.format(
                java.util.Locale.ROOT,
                "%010d 00000 n \n",
                offsets[i]
            ));
        }

        write(out, "trailer\n"
            + "<< /Size 6 /Root 1 0 R >>\n"
            + "startxref\n"
            + xrefOffset
            + "\n%%EOF\n");

        return DelhiveryLabelPdfValidator.validate(
            out.toByteArray()
        );
    }

    private static void write(
            ByteArrayOutputStream out,
            String text) {

        out.writeBytes(
            text.getBytes(StandardCharsets.US_ASCII)
        );
    }
}