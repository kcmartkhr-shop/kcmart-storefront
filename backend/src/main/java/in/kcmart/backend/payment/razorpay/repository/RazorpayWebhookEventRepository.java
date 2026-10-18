package in.kcmart.backend.payment.razorpay.repository;

import in.kcmart.backend.payment.razorpay.entity.RazorpayWebhookEvent;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface RazorpayWebhookEventRepository
        extends JpaRepository<RazorpayWebhookEvent, Long> {

    Optional<RazorpayWebhookEvent> findByEventId(String eventId);

    boolean existsByEventId(String eventId);

    @Modifying
    @Query(
            value = """
                    INSERT INTO razorpay_webhook_events (
                        event_id,
                        event_type,
                        processing_status,
                        razorpay_order_id,
                        razorpay_payment_id,
                        received_at
                    )
                    VALUES (
                        :eventId,
                        :eventType,
                        'RECEIVED',
                        :razorpayOrderId,
                        :razorpayPaymentId,
                        CURRENT_TIMESTAMP
                    )
                    ON CONFLICT (event_id) DO NOTHING
                    """,
            nativeQuery = true
    )
    int insertAuthenticatedEventIfAbsent(
            @Param("eventId") String eventId,
            @Param("eventType") String eventType,
            @Param("razorpayOrderId") String razorpayOrderId,
            @Param("razorpayPaymentId") String razorpayPaymentId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select e
            from RazorpayWebhookEvent e
            where e.id = :id
            """)
    Optional<RazorpayWebhookEvent> findByIdForUpdate(
            @Param("id") Long id);
}