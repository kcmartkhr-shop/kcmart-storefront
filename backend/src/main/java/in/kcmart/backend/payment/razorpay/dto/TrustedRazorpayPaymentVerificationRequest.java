package in.kcmart.backend.payment.razorpay.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TrustedRazorpayPaymentVerificationRequest(

    @NotBlank
    @Email
    @Size(max = 320)
    String email,

    @NotBlank
    @Size(max = 100)
    String razorpayOrderId,

    @NotBlank
    @Size(max = 100)
    String razorpayPaymentId,

    @NotBlank
    @Size(max = 512)
    String razorpaySignature

) {
}