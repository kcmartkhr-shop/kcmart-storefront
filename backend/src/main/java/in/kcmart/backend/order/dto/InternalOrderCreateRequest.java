package in.kcmart.backend.order.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

public record InternalOrderCreateRequest(

    @NotBlank
    @Email
    @Size(max = 320)
    String customerEmail,

    @NotBlank
    @Size(max = 100)
    String idempotencyKey,

    @NotBlank
    @Pattern(
        regexp = "DELIVERY|STORE_PICKUP",
        message = "Invalid fulfillment type"
    )
    String fulfillmentType,

    @NotBlank
    @Pattern(
        regexp = "ONLINE|PAY_AT_STORE",
        message = "Invalid payment method"
    )
    String paymentMethod,

    @NotBlank
    @Size(min = 2, max = 255)
    String customerName,

    @NotBlank
    @Pattern(
        regexp = "^[6-9][0-9]{9}$",
        message = "Invalid Indian mobile number"
    )
    String customerPhone,

    @Size(max = 255)
    String addressLine1,

    @Size(max = 255)
    String addressLine2,

    @Size(max = 255)
    String landmark,

    @Size(max = 120)
    String city,

    @Size(max = 120)
    String state,

    @Size(max = 20)
    String postalCode,

    @Size(max = 80)
    String country,

    @NotEmpty
    @Size(max = 50)
    List<@Valid CheckoutItemRequest> items

) {
}
