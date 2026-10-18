package in.kcmart.backend.shipping.delhivery.controller;

import in.kcmart.backend.shipping.delhivery.service.DelhiveryBookingPreviewService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/orders/{orderId}/delhivery-booking-preview")
public class AdminDelhiveryBookingPreviewController {

    private final DelhiveryBookingPreviewService service;

    public AdminDelhiveryBookingPreviewController(
            DelhiveryBookingPreviewService service) {
        this.service = service;
    }

    @GetMapping
    public DelhiveryBookingPreviewService.Preview preview(
            @PathVariable Long orderId) {
        return service.preview(orderId);
    }
}