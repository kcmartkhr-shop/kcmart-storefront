package in.kcmart.backend.order.event;

import in.kcmart.backend.order.service.OrderNotificationDeliveryService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class OrderCompletionNotificationListener {

    private final OrderNotificationDeliveryService deliveryService;

    public OrderCompletionNotificationListener(
            OrderNotificationDeliveryService deliveryService) {

        this.deliveryService = deliveryService;
    }

    @TransactionalEventListener(
        phase = TransactionPhase.AFTER_COMMIT
    )
    public void handle(
            OrderCompletionNotificationEvent event) {

        deliveryService.deliverCompletionEmail(
            event.notificationId()
        );
    }
}