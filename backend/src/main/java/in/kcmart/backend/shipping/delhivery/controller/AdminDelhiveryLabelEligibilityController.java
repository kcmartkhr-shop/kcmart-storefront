package in.kcmart.backend.shipping.delhivery.controller;

import in.kcmart.backend.shipping.delhivery.service.DelhiveryLabelEligibilityService;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/admin/orders/{orderId}/delhivery-label-eligibility")
public class AdminDelhiveryLabelEligibilityController {

    private final DelhiveryLabelEligibilityService service;

    public AdminDelhiveryLabelEligibilityController(
            DelhiveryLabelEligibilityService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<DelhiveryLabelEligibilityService.LabelEligibilityResult> check(
            @PathVariable Long orderId) {

        if (orderId == null || orderId <= 0) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Valid order ID required."
            );
        }

        return ResponseEntity.ok()
            .cacheControl(CacheControl.noStore())
            .body(service.check(orderId));
    }
}