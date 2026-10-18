package in.kcmart.backend.order.repository;

import in.kcmart.backend.order.entity.Order;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface OrderRepository
    extends JpaRepository<Order, Long> {

    Optional<Order> findByOrderNumber(String orderNumber);

    Optional<Order> findByIdempotencyKey(String idempotencyKey);

    List<Order> findByCustomerIdOrderByCreatedAtDesc(
        Long customerId
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        select orderEntity
        from Order orderEntity
        where orderEntity.id = :orderId
        """)
    Optional<Order> findByIdForUpdate(
        @Param("orderId") Long orderId
    );
}
