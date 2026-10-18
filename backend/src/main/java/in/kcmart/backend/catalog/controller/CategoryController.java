package in.kcmart.backend.catalog.controller;

import in.kcmart.backend.catalog.entity.Category;
import in.kcmart.backend.catalog.repository.CategoryRepository;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/categories")
public class CategoryController {
    private final CategoryRepository repository;

    public CategoryController(CategoryRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public List<Category> getCategories() {
        return repository.findByActiveTrueOrderByNameAsc();
    }
}
