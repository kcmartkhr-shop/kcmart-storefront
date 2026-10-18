package in.kcmart.backend.catalog.controller;

import in.kcmart.backend.catalog.dto.ProductDetailResponse;
import in.kcmart.backend.catalog.dto.ProductImageResponse;
import in.kcmart.backend.catalog.entity.Product;
import in.kcmart.backend.catalog.repository.ProductImageRepository;
import in.kcmart.backend.catalog.repository.ProductRepository;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/v1/products")
public class ProductController {

    private final ProductRepository repository;
    private final ProductImageRepository productImages;

    public ProductController(
            ProductRepository repository,
            ProductImageRepository productImages
    ) {
        this.repository = repository;
        this.productImages = productImages;
    }

    @GetMapping
    public List<Product> getProducts(
            @RequestParam(required = false) Long categoryId
    ) {
        if (categoryId != null) {
            return repository
                    .findByCategoryIdAndActiveTrueOrderByIdDesc(categoryId);
        }

        return repository.findByActiveTrueOrderByIdDesc();
    }

    @GetMapping("/{slug}")
    public ProductDetailResponse getProduct(
            @PathVariable String slug
    ) {
        Product product = repository
                .findBySlugAndActiveTrue(slug)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Product not found"
                ));

        var images = productImages
                .findByProductIdOrderByDisplayOrderAscIdAsc(
                        product.getId()
                )
                .stream()
                .map(ProductImageResponse::from)
                .toList();

        return ProductDetailResponse.from(product, images);
    }
}