package in.kcmart.backend.shipping.delhivery.service;

import in.kcmart.backend.shipping.delhivery.entity.DelhiveryShipmentBookingAttempt;
import in.kcmart.backend.shipping.delhivery.repository.DelhiveryShipmentBookingAttemptRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DelhiveryBookingResultService {

    private final DelhiveryShipmentBookingAttemptRepository attempts;

    public DelhiveryBookingResultService(
            DelhiveryShipmentBookingAttemptRepository attempts) {
        this.attempts = attempts;
    }

    @Transactional
    public void recordSuccess(Long orderId, String waybill) {
        // Direct success updates are intentionally prohibited.
        // Confirmation must atomically update the shipment and booking attempt.
        throw new IllegalStateException(
            "Direct booking success is disabled. " +
            "Use DelhiveryBookingConfirmationService for atomic confirmation."
        );
    }

    @Transactional
    public void recordUnknown(Long orderId, String reason) {
        DelhiveryShipmentBookingAttempt attempt = lockAttempt(orderId);
        attempt.markUnknown(reason);
        attempts.saveAndFlush(attempt);
    }

    @Transactional
    public void recordConfirmedFailure(Long orderId, String reason) {
        DelhiveryShipmentBookingAttempt attempt = lockAttempt(orderId);
        attempt.markFailed(reason);
        attempts.saveAndFlush(attempt);
    }

    private DelhiveryShipmentBookingAttempt lockAttempt(Long orderId) {
        if (orderId == null || orderId <= 0) {
            throw new IllegalArgumentException("Valid order ID required.");
        }

        return attempts.findByOrderIdForUpdate(orderId)
            .orElseThrow(() -> new IllegalStateException(
                "Delhivery booking attempt not found."
            ));
    }
}