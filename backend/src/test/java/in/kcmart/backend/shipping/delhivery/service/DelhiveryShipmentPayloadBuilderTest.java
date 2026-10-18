package in.kcmart.backend.shipping.delhivery.service;

import in.kcmart.backend.order.entity.Order;
import in.kcmart.backend.order.entity.OrderItem;
import in.kcmart.backend.shipping.delhivery.entity.DelhiveryShipmentPackage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DelhiveryShipmentPayloadBuilderTest {

    private DelhiveryShipmentPayloadBuilder builder;
    private Order order;
    private DelhiveryShipmentPackage shipmentPackage;
    private OrderItem item;

    @BeforeEach
    void setup() {
        builder = new DelhiveryShipmentPayloadBuilder(new ObjectMapper());

        order = mock(Order.class);
        shipmentPackage = mock(DelhiveryShipmentPackage.class);
        item = mock(OrderItem.class);

        when(order.getFulfillmentType()).thenReturn("DELIVERY");
        when(order.getOrderStatus()).thenReturn("PACKED");
        when(order.getPaymentMethod()).thenReturn("ONLINE");
        when(order.getPaymentStatus()).thenReturn("PAID");
        when(order.getOrderNumber()).thenReturn("KCMART-TEST-001");
        when(order.getCustomerName()).thenReturn("Test Customer");
        when(order.getCustomerPhone()).thenReturn("9876543210");
        when(order.getAddressLine1()).thenReturn("Test Street");
        when(order.getCity()).thenReturn("Khariar");
        when(order.getState()).thenReturn("Odisha");
        when(order.getPostalCode()).thenReturn("766107");
        when(order.getTotalAmount()).thenReturn(new BigDecimal("1499.00"));

        when(shipmentPackage.getActualWeightGrams()).thenReturn(2000);
        when(shipmentPackage.getLengthCm()).thenReturn(new BigDecimal("15"));
        when(shipmentPackage.getWidthCm()).thenReturn(new BigDecimal("3"));
        when(shipmentPackage.getHeightCm()).thenReturn(new BigDecimal("55"));

        when(item.getProductName()).thenReturn("Test Keyboard");
        when(item.getQuantity()).thenReturn(2);
    }

    @Test
    void buildsSinglePiecePrepaidShipment() {
        String json = builder.build(
            order, shipmentPackage, List.of(item), "KCMART Warehouse");

        assertTrue(json.contains("\"payment_mode\":\"Prepaid\""));
        assertTrue(json.contains("\"weight\":2000"));
        assertTrue(json.contains("\"shipment_height\":55"));
        assertTrue(json.contains("\"KCMART Warehouse\""));
        assertTrue(json.contains("\"quantity\":\"2\""));
        assertFalse(json.contains("\"waybill\""));
    }

    @Test
    void rejectsMissingPickupLocation() {
        assertThrows(IllegalArgumentException.class,
            () -> builder.build(order, shipmentPackage, List.of(item), ""));
    }

    @Test
    void rejectsEmptyItems() {
        assertThrows(IllegalArgumentException.class,
            () -> builder.build(order, shipmentPackage, List.of(), "Warehouse"));
    }

    @Test
    void rejectsUnpaidOrder() {
        when(order.getPaymentStatus()).thenReturn("PENDING");

        assertThrows(IllegalArgumentException.class,
            () -> builder.build(
                order, shipmentPackage, List.of(item), "Warehouse"));
    }

    @Test
    void rejectsInvalidWeight() {
        when(shipmentPackage.getActualWeightGrams()).thenReturn(0);

        assertThrows(IllegalArgumentException.class,
            () -> builder.build(
                order, shipmentPackage, List.of(item), "Warehouse"));
    }
}
