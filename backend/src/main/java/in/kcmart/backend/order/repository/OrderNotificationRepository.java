package in.kcmart.backend.order.repository;

import in.kcmart.backend.order.entity.OrderNotification;
import org.springframework.data.jpa.repository.JpaRepository;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface OrderNotificationRepository
        extends JpaRepository<OrderNotification, Long> {

    List<OrderNotification> findByOrderIdOrderByCreatedAtAscIdAsc(
        Long orderId
    );

    Optional<OrderNotification>
        findByOrderIdAndNotificationTypeAndChannel(
            Long orderId,
            String notificationType,
            String channel
        );

    List<OrderNotification> findByOrderIdAndNotificationTypeAndChannelOrderByCreatedAtDescIdDesc(
        Long orderId,
        String notificationType,
        String channel
    );
    boolean existsByOrderIdAndNotificationTypeAndChannelAndStatus(
        Long orderId,
        String notificationType,
        String channel,
        String status
    );
    Optional<OrderNotification> findByRequestKeyAndNotificationTypeAndChannel(
        String requestKey,
        String notificationType,
        String channel
    );
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select n from OrderNotification n where n.id = :id")
    Optional<OrderNotification> findByIdForUpdate(@Param("id") Long id);
}