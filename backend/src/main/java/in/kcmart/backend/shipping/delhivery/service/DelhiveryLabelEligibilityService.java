package in.kcmart.backend.shipping.delhivery.service;

import in.kcmart.backend.order.repository.OrderShipmentRepository;
import in.kcmart.backend.shipping.delhivery.repository.DelhiveryShipmentBookingAttemptRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DelhiveryLabelEligibilityService {

    private final OrderShipmentRepository shipments;
    private final DelhiveryShipmentBookingAttemptRepository attempts;
    private final DelhiveryLabelEligibilityValidator validator;

    public DelhiveryLabelEligibilityService(
            OrderShipmentRepository shipments,
            DelhiveryShipmentBookingAttemptRepository attempts,
            DelhiveryLabelEligibilityValidator validator) {

        this.shipments = shipments;
        this.attempts = attempts;
        this.validator = validator;
    }

    @Transactional(readOnly = true)
    public LabelEligibilityResult check(Long orderId) {

        if (orderId == null || orderId <= 0) {
            return new LabelEligibilityResult(
                false, "Valid order ID required."
            );
        }

        var shipment = shipments.findByOrderId(orderId).orElse(null);
        var attempt = attempts.findByOrderId(orderId).orElse(null);

        String blocker = validator.validate(
            orderId, shipment, attempt
        );

        return blocker == null
            ? new LabelEligibilityResult(true, null)
            : new LabelEligibilityResult(false, blocker);
    }

    public record LabelEligibilityResult(
        boolean eligible,
        String blocker
    ) {}
}