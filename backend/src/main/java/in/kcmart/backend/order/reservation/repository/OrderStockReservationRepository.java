package in.kcmart.backend.order.reservation.repository;

import in.kcmart.backend.order.reservation.OrderStockReservation;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface OrderStockReservationRepository
        extends JpaRepository<OrderStockReservation, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        select reservation
        from OrderStockReservation reservation
        where reservation.orderId = :orderId
        order by reservation.id
        """)
    List<OrderStockReservation> findByOrderIdForUpdate(
            @Param("orderId") Long orderId
    );

    @Query("""
        select coalesce(sum(reservation.quantity), 0)
        from OrderStockReservation reservation
        where reservation.productId = :productId
          and reservation.status = 'ACTIVE'
          and reservation.expiresAt > :now
        """)
    Integer sumActiveQuantity(
            @Param("productId") Long productId,
            @Param("now") OffsetDateTime now
    );

    @Query("""
        select distinct reservation.orderId
        from OrderStockReservation reservation
        where reservation.status = 'ACTIVE'
          and reservation.expiresAt <= :now
        order by reservation.orderId
        """)
    List<Long> findExpiredOrderIds(
            @Param("now") OffsetDateTime now
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        select reservation
        from OrderStockReservation reservation
        where reservation.status = 'ACTIVE'
          and reservation.expiresAt <= :now
        order by reservation.id
        """)
    List<OrderStockReservation> findExpiredActiveForUpdate(
            @Param("now") OffsetDateTime now
    );

    Optional<OrderStockReservation> findByOrderIdAndProductId(
            Long orderId,
            Long productId
    );
}
