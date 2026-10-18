package in.kcmart.backend.catalog.service;

import in.kcmart.backend.catalog.dto.admin.AdminCategoryRequest;
import in.kcmart.backend.catalog.dto.admin.AdminCategoryResponse;
import in.kcmart.backend.catalog.entity.Category;
import in.kcmart.backend.catalog.repository.CategoryRepository;
import in.kcmart.backend.catalog.repository.ProductRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Locale;

@Service
public class AdminCategoryService {

    private final CategoryRepository categories;
    private final ProductRepository products;

    public AdminCategoryService(
        CategoryRepository categories,
        ProductRepository products
    ) {
        this.categories = categories;
        this.products = products;
    }

    @Transactional(readOnly = true)
    public List<AdminCategoryResponse> findAll() {
        return categories.findAllByOrderByNameAsc()
            .stream()
            .map(category -> AdminCategoryResponse.from(
                category,
                products.countByCategoryId(category.getId())
            ))
            .toList();
    }

    @Transactional(readOnly = true)
    public AdminCategoryResponse findOne(Long id) {
        Category category = getCategory(id);

        return AdminCategoryResponse.from(
            category,
            products.countByCategoryId(id)
        );
    }

    @Transactional
    public AdminCategoryResponse create(AdminCategoryRequest request) {
        String name = normalizeName(request.name());
        String slug = normalizeSlug(request.slug());

        if (categories.existsByNameIgnoreCase(name)) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "A category with this name already exists."
            );
        }

        if (categories.existsBySlugIgnoreCase(slug)) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "A category with this slug already exists."
            );
        }

        validateParent(request.parentId(), null);

        Category category = new Category();
        category.setName(name);
        category.setSlug(slug);
        category.setParentId(request.parentId());
        category.setActive(request.active());

        Category saved = categories.save(category);

        return AdminCategoryResponse.from(saved, 0);
    }

    @Transactional
    public AdminCategoryResponse update(
        Long id,
        AdminCategoryRequest request
    ) {
        Category category = getCategory(id);

        String name = normalizeName(request.name());
        String slug = normalizeSlug(request.slug());

        if (categories.existsByNameIgnoreCaseAndIdNot(name, id)) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "A category with this name already exists."
            );
        }

        if (categories.existsBySlugIgnoreCaseAndIdNot(slug, id)) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "A category with this slug already exists."
            );
        }

        validateParent(request.parentId(), id);

        category.setName(name);
        category.setSlug(slug);
        category.setParentId(request.parentId());
        category.setActive(request.active());

        Category saved = categories.save(category);

        return AdminCategoryResponse.from(
            saved,
            products.countByCategoryId(id)
        );
    }

    @Transactional
    public void delete(Long id) {
        Category category = getCategory(id);

        long productCount = products.countByCategoryId(id);

        if (productCount > 0) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Category cannot be deleted because it contains products. Deactivate it instead."
            );
        }

        boolean usedAsParent = categories.findAll()
            .stream()
            .anyMatch(item -> id.equals(item.getParentId()));

        if (usedAsParent) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Category cannot be deleted because it has child categories."
            );
        }

        categories.delete(category);
    }

    private Category getCategory(Long id) {
        return categories.findById(id)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Category not found."
            ));
    }

    private void validateParent(
        Long parentId,
        Long currentCategoryId
    ) {
        if (parentId == null) {
            return;
        }

        java.util.Set<Long> visited = new java.util.HashSet<>();
        Long ancestorId = parentId;

        while (ancestorId != null) {
            if (!visited.add(ancestorId)) {
                throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Circular category hierarchy detected."
                );
            }

            if (ancestorId.equals(currentCategoryId)) {
                throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "A category cannot be its own descendant."
                );
            }

            Category ancestor = categories.findById(ancestorId)
                .orElseThrow(() -> new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Parent category does not exist."
                ));

            ancestorId = ancestor.getParentId();
        }
    }
    private String normalizeName(String value) {
        return value.trim().replaceAll("\\s+", " ");
    }

    private String normalizeSlug(String value) {
        String slug = value
            .trim()
            .toLowerCase(Locale.ROOT)
            .replaceAll("[^a-z0-9]+", "-")
            .replaceAll("^-+|-+$", "");

        if (slug.isBlank()) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Category slug is invalid."
            );
        }

        return slug;
    }
}