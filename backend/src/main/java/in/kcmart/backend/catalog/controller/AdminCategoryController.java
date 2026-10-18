package in.kcmart.backend.catalog.controller;

import in.kcmart.backend.catalog.dto.admin.AdminCategoryRequest;
import in.kcmart.backend.catalog.dto.admin.AdminCategoryResponse;
import in.kcmart.backend.catalog.service.AdminCategoryService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/categories")
public class AdminCategoryController {

    private final AdminCategoryService service;

    public AdminCategoryController(AdminCategoryService service) {
        this.service = service;
    }

    @GetMapping
    public List<AdminCategoryResponse> findAll() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    public AdminCategoryResponse findOne(
        @PathVariable Long id
    ) {
        return service.findOne(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AdminCategoryResponse create(
        @Valid @RequestBody AdminCategoryRequest request
    ) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    public AdminCategoryResponse update(
        @PathVariable Long id,
        @Valid @RequestBody AdminCategoryRequest request
    ) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
        @PathVariable Long id
    ) {
        service.delete(id);
    }
}