package in.kcmart.backend.order.service;

import in.kcmart.backend.catalog.entity.Product;
import in.kcmart.backend.catalog.entity.ProductInventory;
import in.kcmart.backend.catalog.repository.ProductInventoryRepository;
import in.kcmart.backend.catalog.repository.ProductRepository;
import in.kcmart.backend.customer.entity.Customer;
import in.kcmart.backend.customer.repository.CustomerRepository;
import in.kcmart.backend.order.dto.CheckoutItemRequest;
import in.kcmart.backend.order.dto.InternalOrderCreateRequest;
import in.kcmart.backend.order.dto.OrderCreateResponse;
import in.kcmart.backend.order.entity.Order;
import in.kcmart.backend.order.entity.OrderItem;
import in.kcmart.backend.order.entity.OrderStatusHistory;
import in.kcmart.backend.order.repository.OrderItemRepository;
import in.kcmart.backend.order.repository.OrderRepository;
import in.kcmart.backend.order.repository.OrderStatusHistoryRepository;
import in.kcmart.backend.order.reservation.service.OrderStockReservationService;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class OrderCreationService {

    private static final BigDecimal ZERO =
        new BigDecimal("0.00");

    private final CustomerRepository customers;
    private final ProductRepository products;
    private final ProductInventoryRepository inventory;
    private final OrderRepository orders;
    private final OrderItemRepository orderItems;
    private final OrderStatusHistoryRepository orderStatusHistory;
    private final OrderStockReservationService stockReservations;

    public OrderCreationService(
        CustomerRepository customers,
        ProductRepository products,
        ProductInventoryRepository inventory,
        OrderRepository orders,
        OrderItemRepository orderItems,
        OrderStatusHistoryRepository orderStatusHistory,
        OrderStockReservationService stockReservations
    ) {
        this.customers = customers;
        this.products = products;
        this.inventory = inventory;
        this.orders = orders;
        this.orderItems = orderItems;
        this.orderStatusHistory = orderStatusHistory;
        this.stockReservations = stockReservations;
    }

    @Transactional
    public OrderCreateResponse createStorePickupOrder(
        InternalOrderCreateRequest request
    ) {

        validateRequest(request);

        String email = request.customerEmail().trim();
        String idempotencyKey = request.idempotencyKey().trim();
        String customerName = request.customerName().trim();
        String customerPhone = request.customerPhone().trim();

        String addressLine1 =
            normalizeOptional(request.addressLine1());

        String addressLine2 =
            normalizeOptional(request.addressLine2());

        String landmark =
            normalizeOptional(request.landmark());

        String city =
            normalizeOptional(request.city());

        String state =
            normalizeOptional(request.state());

        String postalCode =
            normalizeOptional(request.postalCode());

        String country =
            normalizeOptional(request.country());

        Customer customer =
            customers.findByEmailIgnoreCaseForUpdate(email)
                .orElseThrow(() ->
                    new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "Customer not found"
                    )
                );

        if (!customer.isActive()) {
            throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "Customer account is inactive"
            );
        }

        Order existing =
            orders.findByIdempotencyKey(idempotencyKey)
                .orElse(null);

        if (existing != null) {
            validateIdempotentReplay(
                existing,
                customer,
                customerName,
                customerPhone,
                request.fulfillmentType(),
                request.paymentMethod(),
                addressLine1,
                addressLine2,
                landmark,
                city,
                state,
                postalCode,
                country,
                request.items()
            );

            return toResponse(existing, true);
        }

        List<CheckoutItemRequest> sortedItems =
            validateAndSortItems(request.items());

        List<PreparedItem> prepared =
            new ArrayList<>();

        BigDecimal subtotal = ZERO;

        for (CheckoutItemRequest requestedItem : sortedItems) {

            Product product =
                products.findById(requestedItem.productId())
                    .orElseThrow(() ->
                        new ResponseStatusException(
                            HttpStatus.BAD_REQUEST,
                            "Product not found"
                        )
                    );

            if (!product.isActive()) {
                throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Product is unavailable"
                );
            }

            BigDecimal unitPrice = product.getPrice();

            if (
                unitPrice == null ||
                unitPrice.signum() < 0
            ) {
                throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Product price is unavailable"
                );
            }

            ProductInventory stock =
                inventory.findByProductIdForUpdate(
                    product.getId()
                )
                .orElseThrow(() ->
                    new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Product inventory is unavailable"
                    )
                );

            boolean delivery =
                "DELIVERY".equals(
                    request.fulfillmentType()
                );

            int availableStock;

            if (delivery) {
                int activeReserved =
                    stockReservations.getActiveReservedQuantity(
                        product.getId()
                    );

                availableStock =
                    stock.getOnlineStock() - activeReserved;
            } else {
                availableStock = stock.getStoreStock();
            }

            if (
                availableStock <
                requestedItem.quantity()
            ) {
                throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    (
                        delivery
                            ? "Insufficient online stock for "
                            : "Insufficient store stock for "
                    ) +
                        product.getName()
                );
            }

            BigDecimal lineTotal =
                unitPrice.multiply(
                    BigDecimal.valueOf(
                        requestedItem.quantity()
                    )
                );

            prepared.add(
                new PreparedItem(
                    product,
                    stock,
                    requestedItem.quantity(),
                    unitPrice,
                    lineTotal
                )
            );

            subtotal = subtotal.add(lineTotal);
        }

        /*
         * All required inventory rows are now locked and validated.
         * Stock mutation starts only after every item has passed.
         */
        boolean delivery =
            "DELIVERY".equals(
                request.fulfillmentType()
            );

        /*
         * DELIVERY + ONLINE:
         * Do not permanently deduct online stock here.
         * A temporary 5-minute reservation is created after
         * the order is saved.
         *
         * STORE_PICKUP:
         * Existing store-stock deduction remains unchanged.
         */
        if (!delivery) {

            for (PreparedItem item : prepared) {

                int remaining =
                    item.inventory().getStoreStock() -
                    item.quantity();

                item.inventory().setStoreStock(
                    remaining
                );
            }
        }

        BigDecimal deliveryCharge = ZERO;
        BigDecimal totalAmount =
            subtotal.add(deliveryCharge);

        Order order = new Order();

        order.setOrderNumber(generateOrderNumber());
        order.setIdempotencyKey(idempotencyKey);

        order.setCustomerId(customer.getId());
        order.setFulfillmentType(
            request.fulfillmentType()
        );

        order.setCustomerName(customerName);
        order.setCustomerEmail(customer.getEmail());
        order.setCustomerPhone(customerPhone);

        /*
         * DELIVERY stores the authoritative checkout
         * address snapshot on the order.
         *
         * STORE_PICKUP intentionally keeps all delivery
         * address fields null.
         */
        if ("DELIVERY".equals(
                request.fulfillmentType())) {

            order.setAddressLine1(addressLine1);
            order.setAddressLine2(addressLine2);
            order.setLandmark(landmark);
            order.setCity(city);
            order.setState(state);
            order.setPostalCode(postalCode);
            order.setCountry(country);

        } else {

            order.setAddressLine1(null);
            order.setAddressLine2(null);
            order.setLandmark(null);
            order.setCity(null);
            order.setState(null);
            order.setPostalCode(null);
            order.setCountry(null);
        }

        order.setSubtotal(subtotal);
        order.setDeliveryCharge(deliveryCharge);
        order.setTotalAmount(totalAmount);

        order.setPaymentMethod(request.paymentMethod());

        /*
         * No money has been collected.
         */
        order.setPaymentStatus("PENDING");

        /*
         * Order exists but has not yet been processed by the store.
         */
        order.setOrderStatus("PENDING");

        Order savedOrder = orders.save(order);

        List<OrderItem> snapshots =
            new ArrayList<>();

        for (PreparedItem item : prepared) {

            OrderItem snapshot = new OrderItem();

            snapshot.setOrderId(savedOrder.getId());
            snapshot.setProductId(
                item.product().getId()
            );
            snapshot.setProductName(
                item.product().getName()
            );
            snapshot.setSku(
                item.product().getSku()
            );
            snapshot.setUnitPrice(
                item.unitPrice()
            );
            snapshot.setQuantity(
                item.quantity()
            );
            snapshot.setLineTotal(
                item.lineTotal()
            );

            snapshots.add(snapshot);
        }

        orderItems.saveAll(snapshots);

        /*
         * Initial lifecycle event belongs to the same transaction
         * as order creation and inventory deduction.
         *
         * If this persistence fails, the complete transaction
         * rolls back.
         */
        OrderStatusHistory initialHistory =
            new OrderStatusHistory();

        initialHistory.setOrderId(savedOrder.getId());
        initialHistory.setPreviousStatus(null);
        initialHistory.setNewStatus("PENDING");
        initialHistory.setActorType("SYSTEM");
        initialHistory.setActorIdentifier("order-creation");
        initialHistory.setNote("Order created.");

        orderStatusHistory.save(initialHistory);

        /*
         * DELIVERY + ONLINE:
         * Create a maximum 5-minute temporary reservation.
         *
         * The reservation is part of this same transaction.
         * If reservation creation fails, the order and its
         * related writes roll back together.
         */
        if (delivery) {

            for (PreparedItem item : prepared) {

                stockReservations.reserveDeliveryStock(
                    savedOrder.getId(),
                    item.product().getId(),
                    item.quantity()
                );
            }
        }

        /*
         * STORE_PICKUP stock was already deducted above.
         *
         * DELIVERY onlineStock has not been permanently deducted.
         * It is held only through order_stock_reservations until
         * payment succeeds or the 5-minute reservation expires.
         */

        return toResponse(savedOrder, false);
    }

    private void validateIdempotentReplay(
        Order existing,
        Customer customer,
        String customerName,
        String customerPhone,
        String fulfillmentType,
        String paymentMethod,
        String addressLine1,
        String addressLine2,
        String landmark,
        String city,
        String state,
        String postalCode,
        String country,
        List<CheckoutItemRequest> requestedItems
    ) {

        if (!existing.getCustomerId().equals(customer.getId())) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Idempotency key is already in use"
            );
        }

        if (
            !fulfillmentType.equals(
                existing.getFulfillmentType()
            ) ||
            !paymentMethod.equals(
                existing.getPaymentMethod()
            )
        ) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Idempotency key does not match this checkout"
            );
        }

        if (
            !customerName.equals(existing.getCustomerName()) ||
            !customerPhone.equals(existing.getCustomerPhone())
        ) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Idempotency key was already used with different customer details"
            );
        }

        if ("DELIVERY".equals(fulfillmentType)) {

            if (
                !same(addressLine1, existing.getAddressLine1()) ||
                !same(addressLine2, existing.getAddressLine2()) ||
                !same(landmark, existing.getLandmark()) ||
                !same(city, existing.getCity()) ||
                !same(state, existing.getState()) ||
                !same(postalCode, existing.getPostalCode()) ||
                !same(country, existing.getCountry())
            ) {
                throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Idempotency key was already used with a different delivery address"
                );
            }
        }

        List<CheckoutItemRequest> normalizedRequestedItems =
            validateAndSortItems(requestedItems);

        List<OrderItem> existingItems =
            orderItems.findByOrderIdOrderByIdAsc(existing.getId());

        if (
            existingItems.size() != normalizedRequestedItems.size()
        ) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Idempotency key was already used with a different cart"
            );
        }

        existingItems.sort(
            Comparator.comparing(OrderItem::getProductId)
        );

        for (int i = 0; i < normalizedRequestedItems.size(); i++) {

            CheckoutItemRequest requested =
                normalizedRequestedItems.get(i);

            OrderItem stored =
                existingItems.get(i);

            if (
                !stored.getProductId().equals(requested.productId()) ||
                stored.getQuantity() != requested.quantity()
            ) {
                throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Idempotency key was already used with a different cart"
                );
            }
        }
    }
    private void validateRequest(
        InternalOrderCreateRequest request
    ) {

        if (request == null) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Order request is required"
            );
        }

        if (
            request.customerEmail() == null ||
            request.customerEmail().isBlank()
        ) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Customer email is required"
            );
        }

        if (
            request.idempotencyKey() == null ||
            request.idempotencyKey().isBlank() ||
            request.idempotencyKey().trim().length() > 100
        ) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Invalid idempotency key"
            );
        }

        String fulfillmentType =
            request.fulfillmentType().trim();

        String paymentMethod =
            request.paymentMethod().trim();

        if (
            !"STORE_PICKUP".equals(fulfillmentType) &&
            !"DELIVERY".equals(fulfillmentType)
        ) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Invalid fulfillment type"
            );
        }

        if (
            !"ONLINE".equals(paymentMethod) &&
            !"PAY_AT_STORE".equals(paymentMethod)
        ) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Invalid payment method"
            );
        }

        if (
            "DELIVERY".equals(fulfillmentType) &&
            !"ONLINE".equals(paymentMethod)
        ) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Home delivery supports online payment only"
            );
        }

        if ("DELIVERY".equals(fulfillmentType)) {
            validateDeliveryAddress(request);
        }

        if (
            request.customerName() == null ||
            request.customerName().trim().length() < 2 ||
            request.customerName().trim().length() > 255
        ) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Invalid customer name"
            );
        }

        if (
            request.customerPhone() == null ||
            !request.customerPhone()
                .trim()
                .matches("^[6-9][0-9]{9}$")
        ) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Invalid customer phone"
            );
        }
    }

    private void validateDeliveryAddress(
        InternalOrderCreateRequest request
    ) {

        if (
            request.addressLine1() == null ||
            request.addressLine1().trim().length() < 5
        ) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Delivery address is required"
            );
        }

        if (
            request.city() == null ||
            request.city().trim().length() < 2
        ) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Delivery city is required"
            );
        }

        if (
            request.state() == null ||
            request.state().trim().length() < 2
        ) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Delivery state is required"
            );
        }

        if (
            request.postalCode() == null ||
            !request.postalCode()
                .trim()
                .matches("^[1-9][0-9]{5}$")
        ) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Invalid delivery postal code"
            );
        }

        if (
            request.country() == null ||
            request.country().trim().length() < 2
        ) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Delivery country is required"
            );
        }
    }

    private String normalizeOptional(String value) {
        if (value == null) {
            return null;
        }

        String normalized = value.trim();

        return normalized.isEmpty()
            ? null
            : normalized;
    }

    private boolean same(String left, String right) {
        if (left == null && right == null) {
            return true;
        }

        if (left == null || right == null) {
            return false;
        }

        return left.equals(right);
    }

    private List<CheckoutItemRequest> validateAndSortItems(
        List<CheckoutItemRequest> requestedItems
    ) {

        if (
            requestedItems == null ||
            requestedItems.isEmpty()
        ) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Cart is empty"
            );
        }

        if (requestedItems.size() > 50) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Too many cart items"
            );
        }

        Set<Long> seen = new HashSet<>();

        List<CheckoutItemRequest> sorted =
            new ArrayList<>();

        for (CheckoutItemRequest item : requestedItems) {

            if (
                item == null ||
                item.productId() == null ||
                item.productId() <= 0
            ) {
                throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid product"
                );
            }

            if (
                item.quantity() < 1 ||
                item.quantity() > 10
            ) {
                throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Quantity must be between 1 and 10"
                );
            }

            if (!seen.add(item.productId())) {
                throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Duplicate product in cart"
                );
            }

            sorted.add(item);
        }

        /*
         * Every transaction locks products in the same product-id
         * order, reducing deadlock risk for multi-item carts.
         */
        sorted.sort(
            Comparator.comparing(
                CheckoutItemRequest::productId
            )
        );

        return sorted;
    }

    private String generateOrderNumber() {

        /*
         * 4 + 32 = 36 characters, below orders.order_number
         * VARCHAR(40). Database uniqueness remains the final guard.
         */
        return "KCM-" +
            UUID.randomUUID()
                .toString()
                .replace("-", "");
    }

    private OrderCreateResponse toResponse(
        Order order,
        boolean replay
    ) {

        return new OrderCreateResponse(
            order.getId(),
            order.getOrderNumber(),
            order.getFulfillmentType(),
            order.getPaymentMethod(),
            order.getPaymentStatus(),
            order.getOrderStatus(),
            order.getSubtotal(),
            order.getDeliveryCharge(),
            order.getTotalAmount(),
            replay
        );
    }

    private record PreparedItem(
        Product product,
        ProductInventory inventory,
        int quantity,
        BigDecimal unitPrice,
        BigDecimal lineTotal
    ) {
    }
}
