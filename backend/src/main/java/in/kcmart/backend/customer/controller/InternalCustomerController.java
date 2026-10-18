package in.kcmart.backend.customer.controller;

import in.kcmart.backend.customer.dto.CustomerResponse;
import in.kcmart.backend.customer.dto.TrustedCustomerSyncRequest;
import in.kcmart.backend.customer.service.CustomerService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/internal/customers")
public class InternalCustomerController {

    private final CustomerService customerService;

    public InternalCustomerController(
        CustomerService customerService
    ) {
        this.customerService = customerService;
    }

    @PostMapping("/sync")
    public ResponseEntity<CustomerResponse> synchronize(
        @Valid
        @RequestBody
        TrustedCustomerSyncRequest request
    ) {
        CustomerResponse customer =
            customerService.synchronizeTrustedIdentity(
                request.provider(),
                request.providerSubject(),
                request.email(),
                request.name(),
                request.imageUrl()
            );

        return ResponseEntity.ok(customer);
    }
}