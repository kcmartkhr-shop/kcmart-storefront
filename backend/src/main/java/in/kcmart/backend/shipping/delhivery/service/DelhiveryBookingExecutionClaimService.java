package in.kcmart.backend.shipping.delhivery.service;

import in.kcmart.backend.shipping.delhivery.repository.DelhiveryShipmentBookingAttemptRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DelhiveryBookingExecutionClaimService {

    private final DelhiveryShipmentBookingAttemptRepository attempts;

    public DelhiveryBookingExecutionClaimService(
            DelhiveryShipmentBookingAttemptRepository attempts) {
        this.attempts = attempts;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void claim(Long orderId) {

        if (orderId == null || orderId <= 0) {
            throw new IllegalArgumentException("Valid order ID required.");
        }

        int updated = attempts.claimForExecution(orderId);

        if (updated != 1) {
            throw new IllegalStateException(
                "Booking execution already claimed or unavailable. " +
                "Do not retry without reconciliation."
            );
        }
    }
}