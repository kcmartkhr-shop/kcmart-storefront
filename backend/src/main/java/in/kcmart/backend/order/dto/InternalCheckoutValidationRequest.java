package in.kcmart.backend.order.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

public record InternalCheckoutValidationRequest(

    @NotBlank
    @Email
    @Size(max = 320)
    String customerEmail,

    @NotBlank
    @Pattern(regexp = "DELIVERY|STORE_PICKUP")
    String fulfillmentType,

    @NotEmpty
    @Size(max = 50)
    List<@Valid CheckoutItemRequest> items

) {
    public CheckoutValidationRequest toCheckoutRequest() {
        return new CheckoutValidationRequest(items);
    }
}