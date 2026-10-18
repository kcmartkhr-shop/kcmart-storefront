package in.kcmart.backend.shipping.delhivery.service;

import org.springframework.stereotype.Service;

@Service
public class DelhiveryBookingOrchestrator {

    private final DelhiveryBookingPreparationService preparation;

    public DelhiveryBookingOrchestrator(
            DelhiveryBookingPreparationService preparation) {
        this.preparation = preparation;
    }

    public String book(Long orderId) {

        // Safety lock: do not activate external booking until
        // serviceability schema and end-to-end behavior are verified.
        throw new IllegalStateException(
            "Delhivery live booking is not activated. " +
            "Serviceability schema and reconciliation must be verified."
        );
    }
}