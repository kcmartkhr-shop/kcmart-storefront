package in.kcmart.backend.customer.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TrustedCustomerSyncRequest(

    @NotBlank
    @Size(max = 30)
    String provider,

    @NotBlank
    @Size(max = 255)
    String providerSubject,

    @NotBlank
    @Email
    @Size(max = 320)
    String email,

    @Size(max = 255)
    String name,

    String imageUrl

) {
}