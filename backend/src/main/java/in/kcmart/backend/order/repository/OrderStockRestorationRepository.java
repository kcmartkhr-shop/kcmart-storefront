package in.kcmart.backend.order.repository;

import in.kcmart.backend.order.entity.OrderStockRestoration;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OrderStockRestorationRepository
        extends JpaRepository<
            OrderStockRestoration,
            Long
        > {

    boolean existsByOrderId(Long orderId);

    Optional<OrderStockRestoration>
        findByOrderId(Long orderId);
}