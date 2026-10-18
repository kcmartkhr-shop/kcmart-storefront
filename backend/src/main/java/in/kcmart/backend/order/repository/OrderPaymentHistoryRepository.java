package in.kcmart.backend.order.repository;

import in.kcmart.backend.order.entity.OrderPaymentHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderPaymentHistoryRepository
        extends JpaRepository<OrderPaymentHistory, Long> {

    List<OrderPaymentHistory> findByOrderIdOrderByCreatedAtAscIdAsc(Long orderId);
}