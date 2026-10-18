package in.kcmart.backend.order.dto.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AdminOrderStatusUpdateRequest(

    @NotBlank(message = "Status is required.")
    String status,

    @Size(
        max = 1000,
        message = "Status note must not exceed 1000 characters."
    )
    String note
) {
}