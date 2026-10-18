package in.kcmart.backend.order.dto.admin;

import jakarta.validation.constraints.Size;

public record AdminPaymentReceivedRequest(

    @Size(
        max = 1000,
        message = "Payment note must not exceed 1000 characters."
    )
    String note

) {
}