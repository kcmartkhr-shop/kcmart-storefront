package in.kcmart.backend.shipping.delhivery.service;

import org.junit.jupiter.api.Test;
import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class DelhiveryPackageWeightCalculatorTest {

    private final DelhiveryPackageWeightCalculator calculator =
        new DelhiveryPackageWeightCalculator();

    @Test
    void calculatesVolumetricWeight() {
        var result = calculator.calculate(
            1200,
            new BigDecimal("30"),
            new BigDecimal("20"),
            new BigDecimal("15"),
            new BigDecimal("5000")
        );

        assertEquals(1200, result.actualWeightGrams());
        assertEquals(1800, result.volumetricWeightGrams());
        assertEquals(1800, result.chargeableWeightGrams());
    }

    @Test
    void usesActualWeightWhenHigher() {
        var result = calculator.calculate(
            3000,
            new BigDecimal("30"),
            new BigDecimal("20"),
            new BigDecimal("15"),
            new BigDecimal("5000")
        );

        assertEquals(3000, result.chargeableWeightGrams());
    }

    @Test
    void roundsVolumetricWeightUpToGram() {
        var result = calculator.calculate(
            100,
            new BigDecimal("10"),
            new BigDecimal("10"),
            new BigDecimal("10.01"),
            new BigDecimal("5000")
        );

        assertEquals(201, result.volumetricWeightGrams());
    }

    @Test
    void rejectsZeroActualWeight() {
        assertThrows(IllegalArgumentException.class, () ->
            calculator.calculate(
                0,
                BigDecimal.ONE,
                BigDecimal.ONE,
                BigDecimal.ONE,
                new BigDecimal("5000")
            )
        );
    }

    @Test
    void rejectsInvalidDimensions() {
        assertThrows(IllegalArgumentException.class, () ->
            calculator.calculate(
                1000,
                new BigDecimal("-1"),
                BigDecimal.ONE,
                BigDecimal.ONE,
                new BigDecimal("5000")
            )
        );
    }

    @Test
    void rejectsInvalidDivisor() {
        assertThrows(IllegalArgumentException.class, () ->
            calculator.calculate(
                1000,
                BigDecimal.ONE,
                BigDecimal.ONE,
                BigDecimal.ONE,
                BigDecimal.ZERO
            )
        );
    }
}