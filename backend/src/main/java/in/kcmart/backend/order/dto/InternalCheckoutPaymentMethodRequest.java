package in.kcmart.backend.order.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record InternalCheckoutPaymentMethodRequest(

    @NotBlank
    @Email
    @Size(max = 320)
    String customerEmail,

    @NotBlank
    @Pattern(regexp = "DELIVERY|STORE_PICKUP")
    String fulfillmentType,

    @NotBlank
    @Pattern(regexp = "ONLINE|PAY_AT_STORE")
    String paymentMethod

) {}