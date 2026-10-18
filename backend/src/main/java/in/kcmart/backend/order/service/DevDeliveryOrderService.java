package in.kcmart.backend.order.service;

import in.kcmart.backend.catalog.entity.Product;
import in.kcmart.backend.catalog.entity.ProductInventory;
import in.kcmart.backend.catalog.repository.ProductInventoryRepository;
import in.kcmart.backend.catalog.repository.ProductRepository;
import in.kcmart.backend.customer.entity.Customer;
import in.kcmart.backend.customer.repository.CustomerRepository;
import in.kcmart.backend.order.entity.Order;
import in.kcmart.backend.order.entity.OrderItem;
import in.kcmart.backend.order.entity.OrderStatusHistory;
import in.kcmart.backend.order.repository.OrderItemRepository;
import in.kcmart.backend.order.repository.OrderRepository;
import in.kcmart.backend.order.repository.OrderStatusHistoryRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.UUID;

@Service
public class DevDeliveryOrderService {

    private static final Long TEST_CUSTOMER_ID = 1L;
    private static final Long TEST_PRODUCT_ID = 1L;

    private final CustomerRepository customers;
    private final ProductRepository products;
    private final ProductInventoryRepository inventory;
    private final OrderRepository orders;
    private final OrderItemRepository orderItems;
    private final OrderStatusHistoryRepository history;

    public DevDeliveryOrderService(
            CustomerRepository customers,
            ProductRepository products,
            ProductInventoryRepository inventory,
            OrderRepository orders,
            OrderItemRepository orderItems,
            OrderStatusHistoryRepository history) {

        this.customers = customers;
        this.products = products;
        this.inventory = inventory;
        this.orders = orders;
        this.orderItems = orderItems;
        this.history = history;
    }

    @Transactional
    public Order createTestDeliveryOrder() {

        Customer customer = customers.findById(TEST_CUSTOMER_ID)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Development test customer not found."
            ));

        if (!customer.isActive()) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Development test customer is inactive."
            );
        }

        Product product = products.findById(TEST_PRODUCT_ID)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Development test product not found."
            ));

        if (!product.isActive()) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Development test product is inactive."
            );
        }

        if (product.getPrice() == null
                || product.getPrice().signum() < 0) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Development test product price is unavailable."
            );
        }

        ProductInventory stock = inventory
            .findByProductIdForUpdate(TEST_PRODUCT_ID)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Development test product inventory is unavailable."
            ));

        if (stock.getOnlineStock() < 1) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Insufficient online stock for development delivery test."
            );
        }

        /*
         * DELIVERY uses online stock.
         * This is the only inventory mutation in this test-order flow.
         */
        stock.setOnlineStock(stock.getOnlineStock() - 1);

        BigDecimal subtotal = product.getPrice();
        BigDecimal deliveryCharge = new BigDecimal("0.00");
        BigDecimal totalAmount = subtotal.add(deliveryCharge);

        Order order = new Order();

        order.setOrderNumber(
            "KCM-DEV-" + UUID.randomUUID()
                .toString()
                .replace("-", "")
                .substring(0, 28)
        );

        order.setIdempotencyKey(
            "DEV-DELIVERY-" + UUID.randomUUID()
        );

        order.setCustomerId(customer.getId());
        order.setFulfillmentType("DELIVERY");

        order.setCustomerName("Development Delivery Test");
        order.setCustomerEmail(customer.getEmail());
        order.setCustomerPhone("9999999999");

        /*
         * Clearly synthetic development-only address.
         */
        order.setAddressLine1("DEV TEST ADDRESS - NOT FOR DISPATCH");
        order.setAddressLine2("KCMART delivery lifecycle test");
        order.setLandmark("DO NOT SHIP");
        order.setCity("Khariar");
        order.setState("Odisha");
        order.setPostalCode("766107");
        order.setCountry("India");

        order.setSubtotal(subtotal);
        order.setDeliveryCharge(deliveryCharge);
        order.setTotalAmount(totalAmount);

        /*
         * Development simulation only.
         * No gateway transaction has occurred.
         */
        order.setPaymentMethod("ONLINE");
        order.setPaymentStatus("PAID");

        order.setOrderStatus("PENDING");

        Order savedOrder = orders.save(order);

        OrderItem item = new OrderItem();
        item.setOrderId(savedOrder.getId());
        item.setProductId(product.getId());
        item.setProductName(product.getName());
        item.setSku(product.getSku());
        item.setUnitPrice(product.getPrice());
        item.setQuantity(1);
        item.setLineTotal(product.getPrice());

        orderItems.save(item);

        OrderStatusHistory initialHistory =
            new OrderStatusHistory();

        initialHistory.setOrderId(savedOrder.getId());
        initialHistory.setPreviousStatus(null);
        initialHistory.setNewStatus("PENDING");
        initialHistory.setActorType("SYSTEM");
        initialHistory.setActorIdentifier(
            "dev-delivery-fixture"
        );
        initialHistory.setNote(
            "Development-only delivery test order. "
                + "ONLINE payment status is simulated; "
                + "no real payment was charged."
        );

        history.save(initialHistory);

        return savedOrder;
    }
}