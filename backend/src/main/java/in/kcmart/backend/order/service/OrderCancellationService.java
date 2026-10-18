package in.kcmart.backend.order.service;

import in.kcmart.backend.catalog.entity.ProductInventory;
import in.kcmart.backend.catalog.repository.ProductInventoryRepository;
import in.kcmart.backend.customer.entity.Customer;
import in.kcmart.backend.customer.repository.CustomerRepository;
import in.kcmart.backend.order.entity.Order;
import in.kcmart.backend.order.entity.OrderItem;
import in.kcmart.backend.order.entity.OrderStatusHistory;
import in.kcmart.backend.order.entity.OrderStockRestoration;
import in.kcmart.backend.order.repository.OrderItemRepository;
import in.kcmart.backend.order.repository.OrderRepository;
import in.kcmart.backend.order.repository.OrderStatusHistoryRepository;
import in.kcmart.backend.order.repository.OrderStockRestorationRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class OrderCancellationService {

    private final OrderRepository orders;
    private final OrderItemRepository items;
    private final ProductInventoryRepository inventory;
    private final OrderStockRestorationRepository restorations;
    private final OrderStatusHistoryRepository history;
    private final CustomerRepository customers;

    public OrderCancellationService(
            OrderRepository orders,
            OrderItemRepository items,
            ProductInventoryRepository inventory,
            OrderStockRestorationRepository restorations,
            OrderStatusHistoryRepository history,
            CustomerRepository customers) {

        this.orders = orders;
        this.items = items;
        this.inventory = inventory;
        this.restorations = restorations;
        this.history = history;
        this.customers = customers;
    }

    /*
     * ADMIN cancellation.
     *
     * Existing behaviour is preserved:
     * ADMIN may cancel only an unpaid PENDING
     * STORE_PICKUP + PAY_AT_STORE order.
     */
    @Transactional
    public Order cancelPendingStorePickup(
            Long orderId,
            String actorIdentifier,
            String note) {

        Order order = lockOrder(orderId);

        return cancelLockedOrder(
            order,
            "ADMIN",
            actorIdentifier,
            note
        );
    }

    /*
     * CUSTOMER cancellation.
     *
     * trustedEmail must come from the trusted
     * server-side Auth.js -> internal API flow.
     *
     * The customer must own the locked order.
     */
    @Transactional
    public Order cancelPendingStorePickupForCustomer(
            String trustedEmail,
            Long orderId,
            String note) {

        Customer customer =
            resolveActiveCustomer(trustedEmail);

        Order order = lockOrder(orderId);

        /*
         * SECURITY:
         *
         * Return the same 404 for a missing order
         * and an order owned by another customer.
         * This avoids leaking order existence.
         */
        if (!customer.getId().equals(
                order.getCustomerId())) {

            throw orderNotFound();
        }

        return cancelLockedOrder(
            order,
            "CUSTOMER",
            customer.getEmail(),
            note
        );
    }

    /*
     * Shared cancellation transaction.
     *
     * The order has already been pessimistically
     * locked before entering this method.
     */
    private Order cancelLockedOrder(
            Order order,
            String actorType,
            String actorIdentifier,
            String note) {

        String currentStatus =
            normalize(order.getOrderStatus());

        String fulfillmentType =
            normalize(order.getFulfillmentType());

        String paymentMethod =
            normalize(order.getPaymentMethod());

        String paymentStatus =
            normalize(order.getPaymentStatus());

        /*
         * Current production cancellation policy:
         *
         * - STORE_PICKUP only
         * - PENDING only
         * - PAY_AT_STORE only
         * - payment must not already be PAID
         *
         * DELIVERY/ONLINE cancellation requires
         * the future refund/payment workflow.
         */
        if (!"STORE_PICKUP".equals(fulfillmentType)) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Delivery cancellation is not available yet."
            );
        }

        if ("CANCELLED".equals(currentStatus)) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Order is already cancelled."
            );
        }

        if (!"PENDING".equals(currentStatus)) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Only PENDING store pickup orders can be cancelled."
            );
        }

        if (!"PAY_AT_STORE".equals(paymentMethod)) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "This order requires the payment/refund cancellation workflow."
            );
        }

        if ("PAID".equals(paymentStatus)) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Paid orders cannot be cancelled through this operation."
            );
        }

        /*
         * Application-level exactly-once guard.
         *
         * V14 UNIQUE(order_id) remains the
         * database-level backstop.
         */
        if (restorations.existsByOrderId(
                order.getId())) {

            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Stock has already been restored for this order."
            );
        }

        List<OrderItem> orderItems =
            items.findByOrderIdOrderByIdAsc(
                order.getId()
            );

        if (orderItems.isEmpty()) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Order has no items to restore."
            );
        }

        /*
         * Current production checkout reserves
         * STORE_PICKUP orders from store_stock.
         */
        for (OrderItem item : orderItems) {

            if (item.getQuantity() <= 0) {
                throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Order contains an invalid item quantity."
                );
            }

            ProductInventory stock =
                inventory.findByProductIdForUpdate(
                    item.getProductId()
                )
                .orElseThrow(() ->
                    new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Product inventory is unavailable for stock restoration."
                    )
                );

            int restoredStock;

            try {
                restoredStock = Math.addExact(
                    stock.getStoreStock(),
                    item.getQuantity()
                );
            } catch (ArithmeticException ex) {
                throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Inventory value overflow during stock restoration."
                );
            }

            stock.setStoreStock(restoredStock);
        }

        /*
         * Restoration ledger, order state and
         * history are all part of this transaction.
         */
        OrderStockRestoration restoration =
            new OrderStockRestoration();

        restoration.setOrderId(order.getId());
        restoration.setFulfillmentType(
            fulfillmentType
        );
        restoration.setRestoredBy(
            normalizeActor(actorIdentifier)
        );

        restorations.save(restoration);

        order.setOrderStatus("CANCELLED");

        Order savedOrder =
            orders.save(order);

        OrderStatusHistory event =
            new OrderStatusHistory();

        event.setOrderId(savedOrder.getId());
        event.setPreviousStatus(currentStatus);
        event.setNewStatus("CANCELLED");
        event.setActorType(actorType);
        event.setActorIdentifier(
            normalizeActor(actorIdentifier)
        );
        event.setNote(normalizeNote(note));

        history.save(event);

        return savedOrder;
    }

    private Order lockOrder(Long orderId) {

        if (orderId == null || orderId <= 0) {
            throw orderNotFound();
        }

        return orders.findByIdForUpdate(orderId)
            .orElseThrow(this::orderNotFound);
    }

    private Customer resolveActiveCustomer(
            String trustedEmail) {

        String email =
            normalizeEmail(trustedEmail);

        Customer customer =
            customers.findByEmailIgnoreCase(email)
                .orElseThrow(() ->
                    new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Customer account not found."
                    )
                );

        if (!customer.isActive()) {
            throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "Customer account is inactive."
            );
        }

        return customer;
    }

    private ResponseStatusException orderNotFound() {
        return new ResponseStatusException(
            HttpStatus.NOT_FOUND,
            "Order not found."
        );
    }

    private String normalizeEmail(String value) {

        if (value == null || value.isBlank()) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Trusted customer email is required."
            );
        }

        String email =
            value.trim().toLowerCase();

        if (email.length() > 320) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Trusted customer email is invalid."
            );
        }

        return email;
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }

        return value.trim().toUpperCase();
    }

    private String normalizeActor(String value) {

        if (value == null || value.isBlank()) {
            return "unknown";
        }

        String normalized = value.trim();

        if (normalized.length() > 320) {
            return normalized.substring(0, 320);
        }

        return normalized;
    }

    private String normalizeNote(String value) {

        if (value == null || value.isBlank()) {
            return null;
        }

        String normalized = value.trim();

        if (normalized.length() > 1000) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Cancellation note must not exceed 1000 characters."
            );
        }

        return normalized;
    }
}