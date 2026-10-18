package in.kcmart.backend.order.event;

import in.kcmart.backend.order.service.OrderNotificationDeliveryService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class OrderShipmentNotificationListener {

    private final OrderNotificationDeliveryService deliveryService;

    public OrderShipmentNotificationListener(
            OrderNotificationDeliveryService deliveryService) {

        this.deliveryService = deliveryService;
    }

    @TransactionalEventListener(
        phase = TransactionPhase.AFTER_COMMIT
    )
    public void onShipmentNotification(
            OrderShipmentNotificationEvent event) {

        deliveryService.deliverShipmentEmail(
            event.notificationId()
        );
    }
}