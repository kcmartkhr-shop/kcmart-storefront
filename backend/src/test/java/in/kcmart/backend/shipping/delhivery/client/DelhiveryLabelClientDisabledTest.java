package in.kcmart.backend.shipping.delhivery.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DelhiveryLabelClientDisabledTest {

    private final DelhiveryLabelClient client =
        new DisabledDelhiveryLabelClient();

    @Test
    void blocksValidWaybillWhenDisabled() {
        IllegalStateException error = assertThrows(
            IllegalStateException.class,
            () -> client.fetchLabelPdf("1234567890123")
        );

        assertTrue(error.getMessage().contains("disabled"));
    }

    @Test
    void blocksNullWaybillWhenDisabled() {
        assertThrows(
            IllegalStateException.class,
            () -> client.fetchLabelPdf(null)
        );
    }

    @Test
    void blocksInvalidWaybillWhenDisabled() {
        assertThrows(
            IllegalStateException.class,
            () -> client.fetchLabelPdf("../unsafe")
        );
    }
}