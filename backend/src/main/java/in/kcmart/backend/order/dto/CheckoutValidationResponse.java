package in.kcmart.backend.order.dto;

import java.math.BigDecimal;
import java.util.List;

public record CheckoutValidationResponse(

    List<ValidatedCheckoutItem> items,
    int itemCount,
    BigDecimal subtotal

) {
}