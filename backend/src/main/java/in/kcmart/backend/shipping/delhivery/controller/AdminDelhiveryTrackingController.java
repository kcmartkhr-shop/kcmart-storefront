package in.kcmart.backend.shipping.delhivery.controller;

import in.kcmart.backend.shipping.delhivery.service.DelhiveryTrackingService;
import in.kcmart.backend.shipping.delhivery.config.DelhiveryProperties;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/admin/delhivery")
public class AdminDelhiveryTrackingController {

    private final DelhiveryTrackingService trackingService;
    private final DelhiveryProperties properties;

    public AdminDelhiveryTrackingController(
            DelhiveryTrackingService trackingService,
            DelhiveryProperties properties) {
        this.trackingService = trackingService;
        this.properties = properties;
    }

    @GetMapping(
        value = "/track/{waybill}",
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<String> track(
            @PathVariable String waybill) {

        if (!properties.isEnabled()) {
            throw new ResponseStatusException(
                HttpStatus.SERVICE_UNAVAILABLE,
                "Delhivery integration is disabled."
            );
        }

        if (waybill == null ||
                !waybill.matches("[A-Za-z0-9-]{5,50}")) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Invalid tracking number."
            );
        }

        return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_JSON)
            .body(trackingService.track(waybill));
    }
}