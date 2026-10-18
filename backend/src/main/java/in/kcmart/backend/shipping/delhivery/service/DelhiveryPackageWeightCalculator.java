package in.kcmart.backend.shipping.delhivery.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.springframework.stereotype.Component;

@Component
public class DelhiveryPackageWeightCalculator {

    public record WeightResult(
        int actualWeightGrams,
        BigDecimal lengthCm,
        BigDecimal widthCm,
        BigDecimal heightCm,
        int volumetricWeightGrams,
        int chargeableWeightGrams
    ) {}

    public WeightResult calculate(
            int actualWeightGrams,
            BigDecimal lengthCm,
            BigDecimal widthCm,
            BigDecimal heightCm,
            BigDecimal volumetricDivisor) {

        if (actualWeightGrams <= 0) {
            throw new IllegalArgumentException(
                "Actual weight must be positive."
            );
        }

        if (lengthCm == null || widthCm == null || heightCm == null
                || lengthCm.signum() <= 0
                || widthCm.signum() <= 0
                || heightCm.signum() <= 0) {
            throw new IllegalArgumentException(
                "Package dimensions must be positive."
            );
        }

        if (volumetricDivisor == null
                || volumetricDivisor.signum() <= 0) {
            throw new IllegalArgumentException(
                "Valid volumetric divisor required."
            );
        }

        if (lengthCm.scale() > 2
                || widthCm.scale() > 2
                || heightCm.scale() > 2
                || lengthCm.precision() - lengthCm.scale() > 8
                || widthCm.precision() - widthCm.scale() > 8
                || heightCm.precision() - heightCm.scale() > 8) {
            throw new IllegalArgumentException(
                "Dimensions exceed supported precision."
            );
        }

        int volumetricGrams;

        try {
            volumetricGrams = lengthCm
                .multiply(widthCm)
                .multiply(heightCm)
                .multiply(BigDecimal.valueOf(1000))
                .divide(volumetricDivisor, 0, RoundingMode.CEILING)
                .intValueExact();
        } catch (ArithmeticException exception) {
            throw new IllegalArgumentException(
                "Calculated weight exceeds supported range.",
                exception
            );
        }

        int chargeableGrams = Math.max(
            actualWeightGrams,
            volumetricGrams
        );

        return new WeightResult(
            actualWeightGrams,
            lengthCm,
            widthCm,
            heightCm,
            volumetricGrams,
            chargeableGrams
        );
    }
}