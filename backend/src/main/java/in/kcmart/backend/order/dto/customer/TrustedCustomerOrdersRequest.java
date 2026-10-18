package in.kcmart.backend.order.dto.customer;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record TrustedCustomerOrdersRequest(

    @NotBlank
    @Email
    String email

) {
}