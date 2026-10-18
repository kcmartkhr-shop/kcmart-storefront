package in.kcmart.backend.order.repository;

import in.kcmart.backend.order.entity.OrderShipment;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface OrderShipmentRepository
        extends JpaRepository<OrderShipment, Long> {

    Optional<OrderShipment> findByOrderId(Long orderId);

    boolean existsByOrderId(Long orderId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        select shipment
        from OrderShipment shipment
        where shipment.orderId = :orderId
        """)
    Optional<OrderShipment> findByOrderIdForUpdate(
        @Param("orderId") Long orderId
    );
}