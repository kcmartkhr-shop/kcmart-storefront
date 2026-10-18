package in.kcmart.backend.payment.razorpay.repository;

import in.kcmart.backend.payment.razorpay.entity.RazorpayPaymentReconciliation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.util.Optional;

public interface RazorpayPaymentReconciliationRepository
        extends JpaRepository<RazorpayPaymentReconciliation, Long> {

    Optional<RazorpayPaymentReconciliation> findByRazorpayPaymentId(
            String razorpayPaymentId
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT r
        FROM RazorpayPaymentReconciliation r
        WHERE r.razorpayPaymentId = :paymentId
    """)
    Optional<RazorpayPaymentReconciliation> findByPaymentIdForUpdate(
            @Param("paymentId") String paymentId
    );
}