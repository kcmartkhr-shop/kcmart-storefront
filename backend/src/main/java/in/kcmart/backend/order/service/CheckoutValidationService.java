package in.kcmart.backend.order.service;

import in.kcmart.backend.catalog.entity.Product;
import in.kcmart.backend.catalog.entity.ProductInventory;
import in.kcmart.backend.catalog.repository.ProductInventoryRepository;
import in.kcmart.backend.catalog.repository.ProductRepository;
import in.kcmart.backend.order.dto.CheckoutItemRequest;
import in.kcmart.backend.order.dto.CheckoutValidationRequest;
import in.kcmart.backend.order.dto.CheckoutValidationResponse;
import in.kcmart.backend.order.dto.ValidatedCheckoutItem;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class CheckoutValidationService {

    private final ProductRepository products;
    private final ProductInventoryRepository inventory;

    public CheckoutValidationService(
        ProductRepository products,
        ProductInventoryRepository inventory
    ) {
        this.products = products;
        this.inventory = inventory;
    }

    @Transactional(readOnly = true)
    public CheckoutValidationResponse validate(
        CheckoutValidationRequest request,
        String fulfillmentType
    ) {

        if (
            !"DELIVERY".equals(fulfillmentType) &&
            !"STORE_PICKUP".equals(fulfillmentType)
        ) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Invalid fulfillment type"
            );
        }

        if (request == null ||
            request.items() == null ||
            request.items().isEmpty()) {

            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Cart is empty"
            );
        }

        Set<Long> seenProductIds = new HashSet<>();

        List<ValidatedCheckoutItem> validatedItems =
            request.items()
                .stream()
                .map(item ->
                    validateItem(
                        item,
                        seenProductIds,
                        fulfillmentType
                    )
                )
                .toList();

        int itemCount =
            validatedItems.stream()
                .mapToInt(
                    ValidatedCheckoutItem::quantity
                )
                .sum();

        BigDecimal subtotal =
            validatedItems.stream()
                .map(
                    ValidatedCheckoutItem::lineTotal
                )
                .reduce(
                    BigDecimal.ZERO,
                    BigDecimal::add
                );

        return new CheckoutValidationResponse(
            List.copyOf(validatedItems),
            itemCount,
            subtotal
        );
    }

    private ValidatedCheckoutItem validateItem(
        CheckoutItemRequest item,
        Set<Long> seenProductIds,
        String fulfillmentType
    ) {

        if (item == null ||
            item.productId() == null ||
            item.productId() <= 0) {

            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Invalid product"
            );
        }

        if (item.quantity() < 1 ||
            item.quantity() > 10) {

            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Quantity must be between 1 and 10"
            );
        }

        if (!seenProductIds.add(item.productId())) {

            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Duplicate product in cart"
            );
        }

        Product product =
            products.findById(item.productId())
                .orElseThrow(() ->
                    new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Product not found"
                    )
                );

        if (!product.isActive()) {

            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Product is unavailable"
            );
        }

        ProductInventory stock =
            inventory.findByProductId(product.getId())
                .orElseThrow(() ->
                    new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Product inventory is unavailable"
                    )
                );

        int availableStock =
                "DELIVERY".equals(fulfillmentType)
                    ? stock.getOnlineStock()
                    : stock.getStoreStock();

            if (availableStock < item.quantity()) {

            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                (
                    "DELIVERY".equals(fulfillmentType)
                        ? "Insufficient online stock for "
                        : "Insufficient store stock for "
                ) +
                    product.getName()
            );
        }

        BigDecimal authoritativePrice =
            product.getPrice();

        if (authoritativePrice == null ||
            authoritativePrice.signum() < 0) {

            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Product price is unavailable"
            );
        }

        BigDecimal lineTotal =
            authoritativePrice.multiply(
                BigDecimal.valueOf(
                    item.quantity()
                )
            );

        return new ValidatedCheckoutItem(
            product.getId(),
            product.getSlug(),
            product.getName(),
            product.getSku(),
            product.getImageUrl(),
            authoritativePrice,
            item.quantity(),
            lineTotal,
            availableStock
        );
    }
}