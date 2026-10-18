package in.kcmart.backend.shipping.delhivery.service;

import in.kcmart.backend.order.entity.Order;
import in.kcmart.backend.order.repository.OrderRepository;
import org.springframework.stereotype.Service;

@Service
public class DelhiveryBookingPreparationService {

    private final OrderRepository orders;
    private final DelhiveryBookingPreflightService preflight;
    private final DelhiveryBookingClaimService claims;

    public DelhiveryBookingPreparationService(
            OrderRepository orders,
            DelhiveryBookingPreflightService preflight,
            DelhiveryBookingClaimService claims) {

        this.orders = orders;
        this.preflight = preflight;
        this.claims = claims;
    }

    public Long prepare(Long orderId) {

        if (orderId == null || orderId <= 0) {
            throw new IllegalArgumentException(
                "Valid order ID required."
            );
        }

        Order order = orders.findById(orderId)
            .orElseThrow(() -> new IllegalStateException(
                "Order not found."
            ));

        preflight.verifyServiceability(order.getPostalCode());

        return claims.claim(orderId);
    }
}