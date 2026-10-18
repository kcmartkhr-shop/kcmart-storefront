package in.kcmart.backend.shipping.delhivery.service;

import in.kcmart.backend.order.entity.Order;
import in.kcmart.backend.shipping.delhivery.entity.DelhiveryShipmentPackage;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.util.Locale;

@Component
public class DelhiveryBookingEligibilityValidator {

    public void validate(
            Order order,
            DelhiveryShipmentPackage shipmentPackage) {

        if (order == null) {
            reject("Order is required.");
        }

        if (!"DELIVERY".equals(normalize(order.getFulfillmentType()))) {
            reject("Only DELIVERY orders can be booked.");
        }

        if (!"PACKED".equals(normalize(order.getOrderStatus()))) {
            reject("Order must be PACKED before booking.");
        }

        if (!"ONLINE".equals(normalize(order.getPaymentMethod()))
                || !"PAID".equals(normalize(order.getPaymentStatus()))) {
            reject("Delhivery booking requires ONLINE and PAID payment.");
        }

        if (blank(order.getOrderNumber())
                || blank(order.getCustomerName())
                || blank(order.getAddressLine1())
                || blank(order.getCity())
                || blank(order.getState())) {
            reject("Complete recipient and delivery address are required.");
        }

        String phone = digits(order.getCustomerPhone());

        if (!phone.matches("[6-9][0-9]{9}")) {
            reject("Valid Indian recipient phone number is required.");
        }

        String pin = order.getPostalCode() == null
                ? ""
                : order.getPostalCode().trim();

        if (!pin.matches("[1-9][0-9]{5}")) {
            reject("Valid six-digit Indian delivery PIN is required.");
        }

        if (shipmentPackage == null) {
            reject("Saved package measurements are required.");
        }

        if (shipmentPackage.getActualWeightGrams() <= 0
                || shipmentPackage.getChargeableWeightGrams() <= 0
                || shipmentPackage.getLengthCm() == null
                || shipmentPackage.getWidthCm() == null
                || shipmentPackage.getHeightCm() == null
                || shipmentPackage.getLengthCm().signum() <= 0
                || shipmentPackage.getWidthCm().signum() <= 0
                || shipmentPackage.getHeightCm().signum() <= 0) {
            reject("Saved package measurements are invalid.");
        }
    }

    private String normalize(String value) {
        return value == null
                ? ""
                : value.trim().toUpperCase(Locale.ROOT);
    }

    private boolean blank(String value) {
        return value == null || value.isBlank();
    }

    private String digits(String value) {
        return value == null
                ? ""
                : value.replaceAll("[^0-9]", "");
    }

    private void reject(String reason) {
        throw new ResponseStatusException(HttpStatus.CONFLICT, reason);
    }
}