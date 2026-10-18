package in.kcmart.backend.payment.razorpay.repository;

import in.kcmart.backend.payment.razorpay.entity.RazorpayPaymentAttempt;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface RazorpayPaymentAttemptRepository
        extends JpaRepository<RazorpayPaymentAttempt, Long> {

    Optional<RazorpayPaymentAttempt> findByRazorpayOrderId(
            String razorpayOrderId
    );

    Optional<RazorpayPaymentAttempt> findByRazorpayPaymentId(
            String razorpayPaymentId
    );

    Optional<RazorpayPaymentAttempt> findFirstByOrderIdAndStatusOrderByIdDesc(
            Long orderId,
            String status
    );

    List<RazorpayPaymentAttempt> findByOrderIdOrderByIdDesc(
            Long orderId
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        select a
        from RazorpayPaymentAttempt a
        where a.orderId = :orderId
          and a.razorpayOrderId = :razorpayOrderId
        """)
    Optional<RazorpayPaymentAttempt> findForVerification(
            @Param("orderId") Long orderId,
            @Param("razorpayOrderId") String razorpayOrderId
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        select a
        from RazorpayPaymentAttempt a
        where a.orderId = :orderId
          and a.razorpayOrderId = :razorpayOrderId
        """)
    Optional<RazorpayPaymentAttempt> findForFinalization(
            @Param("orderId") Long orderId,
            @Param("razorpayOrderId") String razorpayOrderId
    );
}