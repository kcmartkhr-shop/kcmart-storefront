package in.kcmart.backend.shipping.delhivery.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DelhiveryLabelRetrievalFlagTest {

    private DelhiveryProperties properties(
            boolean integrationEnabled,
            boolean shipmentCreationEnabled,
            boolean labelRetrievalEnabled) {

        return new DelhiveryProperties(
            integrationEnabled,
            shipmentCreationEnabled,
            labelRetrievalEnabled,
            "https://track.delhivery.com",
            "",
            ""
        );
    }

    @Test
    void integrationDisabledBlocksLabelRetrieval() {
        assertFalse(properties(false, false, true)
            .isLabelRetrievalEnabled());
    }

    @Test
    void labelRetrievalDisabledBlocksRetrieval() {
        assertFalse(properties(true, false, false)
            .isLabelRetrievalEnabled());
    }

    @Test
    void bothFlagsRequiredForLabelRetrieval() {
        assertTrue(properties(true, false, true)
            .isLabelRetrievalEnabled());
    }

    @Test
    void labelRetrievalDoesNotEnableShipmentCreation() {
        DelhiveryProperties config = properties(true, false, true);

        assertTrue(config.isLabelRetrievalEnabled());
        assertFalse(config.isShipmentCreationEnabled());
    }

    @Test
    void legacyConstructorsKeepLabelRetrievalDisabled() {
        DelhiveryProperties fourParameter = new DelhiveryProperties(
            true,
            "https://track.delhivery.com",
            "",
            ""
        );

        DelhiveryProperties fiveParameter = new DelhiveryProperties(
            true,
            true,
            "https://track.delhivery.com",
            "",
            ""
        );

        assertFalse(fourParameter.isLabelRetrievalEnabled());
        assertFalse(fiveParameter.isLabelRetrievalEnabled());
    }
}