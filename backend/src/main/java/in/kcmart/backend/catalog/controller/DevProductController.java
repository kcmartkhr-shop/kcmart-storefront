package in.kcmart.backend.catalog.controller;

import in.kcmart.backend.catalog.dto.CreateProductRequest;
import in.kcmart.backend.catalog.entity.Product;
import in.kcmart.backend.catalog.service.ProductManagementService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/dev/products")
public class DevProductController {

    private final ProductManagementService service;

    public DevProductController(ProductManagementService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Product create(@Valid @RequestBody CreateProductRequest request) {
        return service.create(request);
    }
}