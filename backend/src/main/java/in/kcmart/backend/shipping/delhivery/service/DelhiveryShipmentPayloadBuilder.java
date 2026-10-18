package in.kcmart.backend.shipping.delhivery.service;

import in.kcmart.backend.order.entity.Order;
import in.kcmart.backend.order.entity.OrderItem;
import in.kcmart.backend.shipping.delhivery.entity.DelhiveryShipmentPackage;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Component
public class DelhiveryShipmentPayloadBuilder {

    private final ObjectMapper mapper;

    public DelhiveryShipmentPayloadBuilder(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    public String build(
            Order order,
            DelhiveryShipmentPackage shipmentPackage,
            List<OrderItem> items,
            String pickupLocation) {

        Objects.requireNonNull(order, "Order is required");
        Objects.requireNonNull(shipmentPackage, "Package is required");
        Objects.requireNonNull(items, "Order items are required");

        if (!"DELIVERY".equals(order.getFulfillmentType())
                || !"PACKED".equals(order.getOrderStatus())
                || !"ONLINE".equals(order.getPaymentMethod())
                || !"PAID".equals(order.getPaymentStatus())) {
            throw new IllegalArgumentException(
                "Only paid, packed online delivery orders can be manifested."
            );
        }

        if (pickupLocation == null || pickupLocation.isBlank()) {
            throw new IllegalArgumentException(
                "Registered Delhivery pickup location is required."
            );
        }

        if (items.isEmpty()) {
            throw new IllegalArgumentException(
                "Shipment must contain at least one order item."
            );
        }

        if (shipmentPackage.getActualWeightGrams() <= 0
                || shipmentPackage.getLengthCm() == null
                || shipmentPackage.getWidthCm() == null
                || shipmentPackage.getHeightCm() == null
                || shipmentPackage.getLengthCm().signum() <= 0
                || shipmentPackage.getWidthCm().signum() <= 0
                || shipmentPackage.getHeightCm().signum() <= 0) {
            throw new IllegalArgumentException(
                "Valid package weight and dimensions are required."
            );
        }

        String products = items.stream()
            .map(item -> {
                if (item.getProductName() == null
                        || item.getProductName().isBlank()
                        || item.getQuantity() <= 0) {
                    throw new IllegalArgumentException(
                        "Valid product name and quantity are required."
                    );
                }
                return item.getProductName().trim();
            })
            .distinct()
            .collect(Collectors.joining(", "));

        int quantity = items.stream()
            .mapToInt(OrderItem::getQuantity)
            .sum();

        String address = List.of(
                nullToEmpty(order.getAddressLine1()),
                nullToEmpty(order.getAddressLine2()),
                nullToEmpty(order.getLandmark()))
            .stream()
            .filter(value -> !value.isBlank())
            .collect(Collectors.joining(", "));

        if (isBlank(order.getOrderNumber())
                || isBlank(order.getCustomerName())
                || isBlank(order.getCustomerPhone())
                || address.isBlank()
                || isBlank(order.getPostalCode())
                || isBlank(order.getCity())
                || isBlank(order.getState())
                || order.getTotalAmount() == null
                || order.getTotalAmount().signum() < 0) {
            throw new IllegalArgumentException(
                "Required shipment order details are missing."
            );
        }

        Map<String, Object> shipment = new LinkedHashMap<>();
        shipment.put("name", order.getCustomerName().trim());
        shipment.put("order", order.getOrderNumber().trim());
        shipment.put("phone", order.getCustomerPhone().trim());
        shipment.put("add", address);
        shipment.put("pin", order.getPostalCode().trim());
        shipment.put("city", order.getCity().trim());
        shipment.put("state", order.getState().trim());
        shipment.put("country", "India");
        shipment.put("payment_mode", "Prepaid");
        shipment.put("cod_amount", BigDecimal.ZERO);
        shipment.put("total_amount", order.getTotalAmount());
        shipment.put("products_desc", products);
        shipment.put("quantity", String.valueOf(quantity));
        shipment.put("weight", shipmentPackage.getActualWeightGrams());
        shipment.put("shipment_length", shipmentPackage.getLengthCm());
        shipment.put("shipment_width", shipmentPackage.getWidthCm());
        shipment.put("shipment_height", shipmentPackage.getHeightCm());
        shipment.put("shipping_mode", "Surface");

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("shipments", List.of(shipment));
        payload.put("pickup_location",
            Map.of("name", pickupLocation.trim()));

        return mapper.writeValueAsString(payload);
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value.trim();
    }
}
