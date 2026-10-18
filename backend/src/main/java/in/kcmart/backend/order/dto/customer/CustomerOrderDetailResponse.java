package in.kcmart.backend.order.dto.customer;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

public record CustomerOrderDetailResponse(
    Long id,
    String orderNumber,
    String fulfillmentType,

    String customerName,
    String customerEmail,
    String customerPhone,

    String addressLine1,
    String addressLine2,
    String landmark,
    String city,
    String state,
    String postalCode,
    String country,

    BigDecimal subtotal,
    BigDecimal deliveryCharge,
    BigDecimal totalAmount,

    String paymentMethod,
    String paymentStatus,
    String orderStatus,

    OffsetDateTime createdAt,
    OffsetDateTime updatedAt,

    List<CustomerOrderItemResponse> items,
    List<CustomerOrderStatusResponse> statusHistory,
    CustomerOrderShipmentResponse shipment,
    CustomerOrderInvoiceResponse invoice
) {
}