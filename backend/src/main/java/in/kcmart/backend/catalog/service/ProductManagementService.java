package in.kcmart.backend.catalog.service;

import in.kcmart.backend.catalog.dto.*;
import in.kcmart.backend.catalog.entity.*;
import in.kcmart.backend.catalog.repository.*;
import in.kcmart.backend.order.reservation.repository.OrderStockReservationRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class ProductManagementService {
    private final OrderStockReservationRepository reservationRepository;

    private final ProductRepository products;
    private final CategoryRepository categories;
    private final ProductInventoryRepository inventory;

    public ProductManagementService(OrderStockReservationRepository reservationRepository, 
            ProductRepository products,
            CategoryRepository categories,
            ProductInventoryRepository inventory) {
        this.reservationRepository = reservationRepository;
        this.products = products;
        this.categories = categories;
        this.inventory = inventory;
    }

    private void validateCategory(Long categoryId) {
        var category = categories.findById(categoryId)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.BAD_REQUEST, "Category not found"
            ));

        if (!category.isActive()) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST, "Category is inactive"
            );
        }
    }

    private void validatePrice(
            java.math.BigDecimal price,
            java.math.BigDecimal mrp) {
        if (mrp != null && mrp.compareTo(price) < 0) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "MRP cannot be lower than selling price"
            );
        }
    }

    private Product requireProduct(Long id) {
        return products.findById(id)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Product not found"
            ));
    }

    private AdminProductResponse response(Product product) {
        var stock = inventory.findById(product.getId())
            .orElse(null);

        return AdminProductResponse.from(product, stock);
    }

    @Transactional(readOnly = true)
    public List<AdminProductResponse> listAll() {
        return products.findAll().stream()
            .map(this::response)
            .toList();
    }

    @Transactional(readOnly = true)
    public AdminProductResponse getById(Long id) {
        return response(requireProduct(id));
    }

    @Transactional
    public Product create(CreateProductRequest request) {
        validateCategory(request.categoryId());
        validatePrice(request.price(), request.mrp());

        Product product = new Product();
        product.setCategoryId(request.categoryId());
        product.setName(request.name());
        product.setSlug(request.slug());
        product.setDescription(request.description());
        product.setBrand(request.brand());
        product.setSku(request.sku());
        product.setPrice(request.price());
        product.setMrp(request.mrp());
        product.setImageUrl(request.imageUrl());
        product.setActive(true);

        product = products.saveAndFlush(product);

        ProductInventory stock = new ProductInventory();
        stock.setProductId(product.getId());
        stock.setOnlineStock(request.onlineStock());
        stock.setStoreStock(request.storeStock());

        inventory.save(stock);

        return product;
    }

    @Transactional
    public AdminProductResponse update(
            Long id,
            UpdateProductRequest request) {

        Product product = requireProduct(id);

        validateCategory(request.categoryId());
        validatePrice(request.price(), request.mrp());

        product.setCategoryId(request.categoryId());
        product.setName(request.name());
        product.setSlug(request.slug());
        product.setDescription(request.description());
        product.setImageUrl(request.imageUrl());
        product.setBrand(request.brand());
        product.setSku(request.sku());
        product.setPrice(request.price());
        product.setMrp(request.mrp());

        return response(products.saveAndFlush(product));
    }

    @Transactional
    public AdminProductResponse updateInventory(
            Long id,
            UpdateInventoryRequest request) {

        requireProduct(id);

        ProductInventory stock = inventory.findByProductIdForUpdate(id)
            .orElseGet(() -> {
                ProductInventory created = new ProductInventory();
                created.setProductId(id);
                return created;
            });

        if (request.expectedOnlineStock() != null
                && request.expectedStoreStock() != null) {

            if (stock.getOnlineStock() != request.expectedOnlineStock()
                    || stock.getStoreStock() != request.expectedStoreStock()) {

                throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Inventory changed since it was loaded. Refresh and try again."
                );
            }
        }

        Integer activeReserved = reservationRepository.sumActiveQuantity(
                id,
                java.time.OffsetDateTime.now()
        );

        int reservedQuantity = activeReserved == null ? 0 : activeReserved;

        if (request.onlineStock() < reservedQuantity) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Online stock cannot be lower than active customer reservations."
            );
        }

        stock.setOnlineStock(request.onlineStock());
        stock.setStoreStock(request.storeStock());

        inventory.saveAndFlush(stock);

        return response(requireProduct(id));
    }

    @Transactional
    public AdminProductResponse setActive(
            Long id,
            boolean active) {

        Product product = requireProduct(id);
        product.setActive(active);

        return response(products.saveAndFlush(product));
    }
}