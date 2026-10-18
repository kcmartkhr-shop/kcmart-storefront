package in.kcmart.backend.order.repository;

import in.kcmart.backend.order.entity.OrderStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderStatusHistoryRepository
    extends JpaRepository<OrderStatusHistory, Long> {

    List<OrderStatusHistory>
        findByOrderIdOrderByCreatedAtAscIdAsc(Long orderId);
}