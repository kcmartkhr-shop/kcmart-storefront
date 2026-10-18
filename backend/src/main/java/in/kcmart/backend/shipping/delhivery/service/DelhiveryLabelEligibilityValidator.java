package in.kcmart.backend.shipping.delhivery.service;

import in.kcmart.backend.order.entity.OrderShipment;
import in.kcmart.backend.shipping.delhivery.entity.DelhiveryShipmentBookingAttempt;
import org.springframework.stereotype.Component;

@Component
public class DelhiveryLabelEligibilityValidator {

    public String validate(
            Long orderId,
            OrderShipment shipment,
            DelhiveryShipmentBookingAttempt attempt) {

        if (orderId == null || orderId <= 0) {
            return "Valid order ID required.";
        }

        if (shipment == null) {
            return "Order shipment record not found.";
        }

        if (!orderId.equals(shipment.getOrderId())) {
            return "Shipment does not belong to the requested order.";
        }

        if (!"Delhivery".equalsIgnoreCase(shipment.getCarrierName())) {
            return "Shipment carrier must be Delhivery.";
        }

        String awb = shipment.getTrackingNumber();

        if (awb == null || !awb.matches("[A-Za-z0-9-]{5,50}")) {
            return "Confirmed Delhivery AWB required.";
        }

        if (attempt == null) {
            return "Delhivery booking attempt not found.";
        }

        if (!orderId.equals(attempt.getOrderId())) {
            return "Booking attempt does not belong to the requested order.";
        }

        if (!"SUCCEEDED".equals(attempt.getAttemptStatus())) {
            return "Delhivery booking is not confirmed.";
        }

        if (!awb.equals(attempt.getWaybill())) {
            return "Shipment AWB does not match booking confirmation.";
        }

        return null;
    }
}