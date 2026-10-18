package in.kcmart.backend.order.reservation.service;

import in.kcmart.backend.order.reservation.repository.OrderStockReservationRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;

@Service
@ConditionalOnProperty(name = "kcmart.reservation.expiry.enabled", havingValue = "true", matchIfMissing = true)
public class DeliveryReservationExpiryService {

    private static final long FROZEN_ORDER_ID_MAX = 10L;

    private final OrderStockReservationRepository reservations;
    private final DeliveryReservationExpiryProcessor processor;

    public DeliveryReservationExpiryService(
            OrderStockReservationRepository reservations,
            DeliveryReservationExpiryProcessor processor) {
        this.reservations = reservations;
        this.processor = processor;
    }

    @Scheduled(fixedDelay = 30000)
    public void expireReservations() {
        List<Long> expiredOrderIds =
                reservations.findExpiredOrderIds(OffsetDateTime.now());

        for (Long orderId : expiredOrderIds) {
            if (orderId == null || orderId <= FROZEN_ORDER_ID_MAX) {
                continue;
            }

            processor.expireOneOrder(orderId);
        }
    }
}