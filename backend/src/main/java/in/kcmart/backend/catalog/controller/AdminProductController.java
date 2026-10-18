package in.kcmart.backend.catalog.controller;

import in.kcmart.backend.catalog.dto.*;
import in.kcmart.backend.catalog.service.ProductManagementService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/products")
public class AdminProductController {

    private final ProductManagementService service;

    public AdminProductController(ProductManagementService service) {
        this.service = service;
    }


    @PostMapping
    @ResponseStatus(org.springframework.http.HttpStatus.CREATED)
    public in.kcmart.backend.catalog.entity.Product create(
            @Valid @RequestBody CreateProductRequest request) {
        return service.create(request);
    }
    @GetMapping
    public List<AdminProductResponse> list() {
        return service.listAll();
    }

    @GetMapping("/{id}")
    public AdminProductResponse get(@PathVariable Long id) {
        return service.getById(id);
    }

    @PutMapping("/{id}")
    public AdminProductResponse update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateProductRequest request) {
        return service.update(id, request);
    }

    @PutMapping("/{id}/inventory")
    public AdminProductResponse updateInventory(
            @PathVariable Long id,
            @Valid @RequestBody UpdateInventoryRequest request) {
        return service.updateInventory(id, request);
    }

    @PatchMapping("/{id}/active")
    public AdminProductResponse setActive(
            @PathVariable Long id,
            @RequestParam boolean active) {
        return service.setActive(id, active);
    }
}