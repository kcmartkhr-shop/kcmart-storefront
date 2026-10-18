package in.kcmart.backend.shipping.delhivery.controller;

import in.kcmart.backend.shipping.delhivery.entity.DelhiveryShipmentPackage;
import in.kcmart.backend.shipping.delhivery.service.DelhiveryPackageService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/v1/admin/orders/{orderId}/delhivery-package")
public class AdminDelhiveryPackageController {

    private final DelhiveryPackageService service;

    public AdminDelhiveryPackageController(
            DelhiveryPackageService service) {
        this.service = service;
    }

    public record PackageRequest(
        @Positive int actualWeightGrams,

        @NotNull
        @DecimalMin(value = "0.00", inclusive = false)
        @Digits(integer = 8, fraction = 2)
        BigDecimal lengthCm,

        @NotNull
        @DecimalMin(value = "0.00", inclusive = false)
        @Digits(integer = 8, fraction = 2)
        BigDecimal widthCm,

        @NotNull
        @DecimalMin(value = "0.00", inclusive = false)
        @Digits(integer = 8, fraction = 2)
        BigDecimal heightCm
    ) {}

    public record PackageResponse(
        Long id,
        Long orderId,
        int actualWeightGrams,
        BigDecimal lengthCm,
        BigDecimal widthCm,
        BigDecimal heightCm,
        int volumetricWeightGrams,
        int chargeableWeightGrams
    ) {}

    @GetMapping
    public PackageResponse get(
            @PathVariable Long orderId) {

        return toResponse(service.getByOrderId(orderId));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PackageResponse create(
            @PathVariable Long orderId,
            @Valid @RequestBody PackageRequest request) {

        return toResponse(service.create(
            orderId,
            request.actualWeightGrams(),
            request.lengthCm(),
            request.widthCm(),
            request.heightCm()
        ));
    }

    private PackageResponse toResponse(
            DelhiveryShipmentPackage value) {

        return new PackageResponse(
            value.getId(),
            value.getOrderId(),
            value.getActualWeightGrams(),
            value.getLengthCm(),
            value.getWidthCm(),
            value.getHeightCm(),
            value.getVolumetricWeightGrams(),
            value.getChargeableWeightGrams()
        );
    }
}