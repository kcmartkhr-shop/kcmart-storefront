package in.kcmart.backend.payment.razorpay.repository;

import in.kcmart.backend.payment.razorpay.entity.RazorpayRefundAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface RazorpayRefundAttemptRepository
        extends JpaRepository<RazorpayRefundAttempt, Long> {

    Optional<RazorpayRefundAttempt> findByReconciliationId(
            Long reconciliationId
    );

    Optional<RazorpayRefundAttempt> findByIdempotencyKey(
            String idempotencyKey
    );
}