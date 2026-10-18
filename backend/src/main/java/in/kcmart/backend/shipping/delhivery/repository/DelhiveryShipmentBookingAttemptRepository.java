package in.kcmart.backend.shipping.delhivery.repository;

import in.kcmart.backend.shipping.delhivery.entity.DelhiveryShipmentBookingAttempt;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface DelhiveryShipmentBookingAttemptRepository
        extends JpaRepository<DelhiveryShipmentBookingAttempt, Long> {

    Optional<DelhiveryShipmentBookingAttempt> findByOrderId(Long orderId);

    boolean existsByOrderId(Long orderId);

    @org.springframework.data.jpa.repository.Modifying
    @Query("""
        update DelhiveryShipmentBookingAttempt attempt
        set attempt.attemptStatus = 'PROCESSING',
            attempt.updatedAt = CURRENT_TIMESTAMP
        where attempt.orderId = :orderId
          and attempt.attemptStatus = 'PENDING'
        """)
    int claimForExecution(@Param("orderId") Long orderId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        select attempt
        from DelhiveryShipmentBookingAttempt attempt
        where attempt.orderId = :orderId
        """)
    Optional<DelhiveryShipmentBookingAttempt> findByOrderIdForUpdate(
        @Param("orderId") Long orderId
    );
}