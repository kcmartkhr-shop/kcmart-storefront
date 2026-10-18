package in.kcmart.backend.shipping.delhivery.mock;

import org.junit.jupiter.api.Test;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class MockShippingLabelPdfFactoryTest {

    @Test
    void generatesPdfHeader() {
        byte[] pdf = MockShippingLabelPdfFactory.create(
            "TEST123456789"
        );

        String text = new String(
            pdf, StandardCharsets.US_ASCII
        );

        assertTrue(text.startsWith("%PDF-1.4"));
    }

    @Test
    void containsVisibleTestWarning() {
        String text = new String(
            MockShippingLabelPdfFactory.create("TEST123456789"),
            StandardCharsets.US_ASCII
        );

        assertTrue(text.contains(
            "KCMART TEST LABEL - NOT FOR SHIPPING"
        ));
        assertTrue(text.contains(
            "NOT A REAL DELHIVERY LABEL"
        ));
    }

    @Test
    void includesTestWaybill() {
        String text = new String(
            MockShippingLabelPdfFactory.create("TEST123456789"),
            StandardCharsets.US_ASCII
        );

        assertTrue(text.contains("TEST AWB: TEST123456789"));
    }

    @Test
    void rejectsNullWaybill() {
        assertThrows(
            IllegalArgumentException.class,
            () -> MockShippingLabelPdfFactory.create(null)
        );
    }

    @Test
    void rejectsUnsafeWaybill() {
        assertThrows(
            IllegalArgumentException.class,
            () -> MockShippingLabelPdfFactory.create("../unsafe")
        );
    }

    @Test
    void generatesValidPdfCrossReference() {
        String text = new String(
            MockShippingLabelPdfFactory.create("TEST123456789"),
            StandardCharsets.US_ASCII
        );

        int xrefPosition = text.indexOf("xref\n0 6\n");
        int startxrefPosition = text.lastIndexOf(
            "startxref\n"
        );

        assertTrue(xrefPosition > 0);
        assertTrue(startxrefPosition > xrefPosition);

        String pointer = text.substring(
            startxrefPosition + "startxref\n".length()
        ).split("\n")[0];

        assertEquals(
            xrefPosition,
            Integer.parseInt(pointer.trim())
        );
    }
}