package in.kcmart.backend.shipping.delhivery.config;

import java.math.BigDecimal;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class DelhiveryPackageConfiguration {

    private final BigDecimal volumetricDivisor;

    public DelhiveryPackageConfiguration(
            @Value("${delhivery.package.volumetric-divisor:5000}")
            BigDecimal volumetricDivisor) {

        if (volumetricDivisor == null
                || volumetricDivisor.signum() <= 0) {
            throw new IllegalArgumentException(
                "Delhivery volumetric divisor must be positive."
            );
        }

        this.volumetricDivisor = volumetricDivisor;
    }

    public BigDecimal getVolumetricDivisor() {
        return volumetricDivisor;
    }
}