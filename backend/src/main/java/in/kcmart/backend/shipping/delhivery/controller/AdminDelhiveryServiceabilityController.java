package in.kcmart.backend.shipping.delhivery.controller;

import in.kcmart.backend.shipping.delhivery.config.DelhiveryProperties;
import in.kcmart.backend.shipping.delhivery.service.DelhiveryServiceabilityGate;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/delhivery")
public class AdminDelhiveryServiceabilityController {

    private final DelhiveryProperties properties;
    private final DelhiveryServiceabilityGate gate;

    public AdminDelhiveryServiceabilityController(
            DelhiveryProperties properties,
            DelhiveryServiceabilityGate gate) {
        this.properties = properties;
        this.gate = gate;
    }

    @GetMapping("/serviceability/{pincode}")
    public Map<String, Object> check(@PathVariable String pincode) {

        if (pincode == null || !pincode.matches("[1-9][0-9]{5}")) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Enter a valid six-digit Indian pincode."
            );
        }

        if (!properties.isEnabled()) {
            throw new ResponseStatusException(
                HttpStatus.SERVICE_UNAVAILABLE,
                "Delhivery integration is currently disabled."
            );
        }

        try {
            gate.requireServiceable(pincode);
        } catch (IllegalStateException ex) {
            throw new ResponseStatusException(
                HttpStatus.SERVICE_UNAVAILABLE,
                "Delhivery serviceability could not be confirmed."
            );
        }

        return Map.of(
            "pincode", pincode,
            "serviceable", true,
            "prepaid", true,
            
            "shipmentBooked", false
        );
    }
}