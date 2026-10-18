package in.kcmart.backend.shipping.delhivery.service;

import in.kcmart.backend.order.entity.OrderShipment;
import in.kcmart.backend.order.repository.OrderShipmentRepository;
import in.kcmart.backend.shipping.delhivery.entity.DelhiveryShipmentBookingAttempt;
import in.kcmart.backend.shipping.delhivery.repository.DelhiveryShipmentBookingAttemptRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Service
public class DelhiveryBookingConfirmationService {

    private final DelhiveryShipmentBookingAttemptRepository attempts;
    private final OrderShipmentRepository shipments;

    public DelhiveryBookingConfirmationService(
            DelhiveryShipmentBookingAttemptRepository attempts,
            OrderShipmentRepository shipments) {
        this.attempts = attempts;
        this.shipments = shipments;
    }

    @Transactional
    public void confirm(Long orderId, String waybill) {

        if (orderId == null || orderId <= 0) {
            throw new IllegalArgumentException(
                "Valid order ID required."
            );
        }

        if (waybill == null ||
                !waybill.matches("[A-Za-z0-9-]{5,50}")) {
            throw new IllegalArgumentException(
                "Invalid Delhivery waybill."
            );
        }

        DelhiveryShipmentBookingAttempt attempt =
            attempts.findByOrderIdForUpdate(orderId)
                .orElseThrow(() -> new IllegalStateException(
                    "Delhivery booking attempt not found."
                ));

        if (!"PROCESSING".equals(attempt.getAttemptStatus())) {
            throw new IllegalStateException(
                "Booking confirmation requires PROCESSING status."
            );
        }

        OrderShipment shipment =
            shipments.findByOrderIdForUpdate(orderId)
                .orElseThrow(() -> new IllegalStateException(
                    "Order shipment record not found."
                ));

        if (!"PACKED".equals(shipment.getShipmentStatus())) {
            throw new IllegalStateException(
                "Shipment must be PACKED before booking confirmation."
            );
        }

        if (shipment.getTrackingNumber() != null
                && !shipment.getTrackingNumber().isBlank()) {
            throw new IllegalStateException(
                "Shipment already has a tracking number."
            );
        }

        if (shipment.getCarrierName() != null
                && !shipment.getCarrierName().isBlank()) {
            throw new IllegalStateException(
                "Shipment already has a carrier."
            );
        }

        shipment.setCarrierName("Delhivery");
        shipment.setTrackingNumber(waybill);
        shipment.setUpdatedBy("DELHIVERY_INTEGRATION");
        shipment.setUpdatedAt(OffsetDateTime.now());

        attempt.markSucceeded(waybill);

        shipments.saveAndFlush(shipment);
        attempts.saveAndFlush(attempt);
    }
}