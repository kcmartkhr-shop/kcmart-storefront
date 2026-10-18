package in.kcmart.backend.order.reservation.service;

import in.kcmart.backend.catalog.entity.ProductInventory;
import in.kcmart.backend.catalog.repository.ProductInventoryRepository;
import in.kcmart.backend.order.reservation.OrderStockReservation;
import in.kcmart.backend.order.reservation.repository.OrderStockReservationRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.List;

@Service
public class OrderStockReservationService {

    private static final int RESERVATION_MINUTES = 5;

    private static final String ACTIVE = "ACTIVE";
    private static final String CONSUMED = "CONSUMED";
    private static final String RELEASED = "RELEASED";

    private final ProductInventoryRepository inventoryRepository;
    private final OrderStockReservationRepository reservationRepository;

    public OrderStockReservationService(
            ProductInventoryRepository inventoryRepository,
            OrderStockReservationRepository reservationRepository) {

        this.inventoryRepository = inventoryRepository;
        this.reservationRepository = reservationRepository;
    }

    @Transactional(readOnly = true)
    public int getActiveReservedQuantity(Long productId) {

        if (productId == null || productId <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid product id"
            );
        }

        Integer reserved =
                reservationRepository.sumActiveQuantity(
                        productId,
                        OffsetDateTime.now()
                );

        return reserved == null ? 0 : reserved;
    }
    @Transactional
    public OrderStockReservation reserveDeliveryStock(
            Long orderId,
            Long productId,
            int quantity) {

        if (orderId == null || orderId <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid order id"
            );
        }

        if (productId == null || productId <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid product id"
            );
        }

        if (quantity < 1 || quantity > 10) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid reservation quantity"
            );
        }

        OffsetDateTime now = OffsetDateTime.now();

        ProductInventory inventory =
                inventoryRepository.findByProductIdForUpdate(productId)
                        .orElseThrow(() ->
                                new ResponseStatusException(
                                        HttpStatus.CONFLICT,
                                        "Inventory not found for product"
                                )
                        );

        int activeReserved =
                reservationRepository.sumActiveQuantity(
                        productId,
                        now
                );

        int available =
                inventory.getOnlineStock() - activeReserved;

        if (available < quantity) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Insufficient online stock"
            );
        }

        if (reservationRepository
                .findByOrderIdAndProductId(orderId, productId)
                .isPresent()) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Stock reservation already exists for this order"
            );
        }

        OrderStockReservation reservation =
                new OrderStockReservation();

        reservation.setOrderId(orderId);
        reservation.setProductId(productId);
        reservation.setQuantity(quantity);
        reservation.setStatus(ACTIVE);
        reservation.setExpiresAt(
                now.plusMinutes(RESERVATION_MINUTES)
        );

        return reservationRepository.save(reservation);
    }

    @Transactional
    public void consumeForPaidOrder(Long orderId) {

        if (orderId == null || orderId <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid order id"
            );
        }

        OffsetDateTime now = OffsetDateTime.now();

        List<OrderStockReservation> reservations =
                reservationRepository.findByOrderIdForUpdate(orderId);

        if (reservations.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "No stock reservation exists for this order"
            );
        }

        for (OrderStockReservation reservation : reservations) {

            if (CONSUMED.equals(reservation.getStatus())) {
                continue;
            }

            if (!ACTIVE.equals(reservation.getStatus())) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Stock reservation is no longer active"
                );
            }

            if (!reservation.getExpiresAt().isAfter(now)) {
                reservation.setStatus(RELEASED);
                reservation.setReleasedAt(now);

                reservationRepository.save(reservation);

                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Stock reservation has expired"
                );
            }

            ProductInventory inventory =
                    inventoryRepository
                            .findByProductIdForUpdate(
                                    reservation.getProductId()
                            )
                            .orElseThrow(() ->
                                    new ResponseStatusException(
                                            HttpStatus.CONFLICT,
                                            "Inventory not found for reserved product"
                                    )
                            );

            int remaining =
                    inventory.getOnlineStock()
                            - reservation.getQuantity();

            if (remaining < 0) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Insufficient online stock while consuming reservation"
                );
            }

            inventory.setOnlineStock(remaining);

            reservation.setStatus(CONSUMED);
            reservation.setConsumedAt(now);

            reservationRepository.save(reservation);
            inventoryRepository.save(inventory);
        }
    }

    @Transactional
    public int releaseExpiredReservations() {

        OffsetDateTime now = OffsetDateTime.now();

        List<OrderStockReservation> expired =
                reservationRepository.findExpiredActiveForUpdate(now);

        for (OrderStockReservation reservation : expired) {

            reservation.setStatus(RELEASED);
            reservation.setReleasedAt(now);

            reservationRepository.save(reservation);
        }

        return expired.size();
    }
}
