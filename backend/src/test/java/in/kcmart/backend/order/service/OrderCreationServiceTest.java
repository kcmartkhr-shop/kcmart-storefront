package in.kcmart.backend.order.service;

import in.kcmart.backend.catalog.repository.ProductInventoryRepository;
import in.kcmart.backend.catalog.repository.ProductRepository;
import in.kcmart.backend.customer.entity.Customer;
import in.kcmart.backend.customer.repository.CustomerRepository;
import in.kcmart.backend.order.dto.CheckoutItemRequest;
import in.kcmart.backend.order.dto.InternalOrderCreateRequest;
import in.kcmart.backend.order.dto.OrderCreateResponse;
import in.kcmart.backend.order.entity.Order;
import in.kcmart.backend.order.entity.OrderItem;
import in.kcmart.backend.order.repository.OrderItemRepository;
import in.kcmart.backend.order.repository.OrderRepository;
import in.kcmart.backend.order.repository.OrderStatusHistoryRepository;
import in.kcmart.backend.order.reservation.service.OrderStockReservationService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class OrderCreationServiceTest {

    private CustomerRepository customers;
    private ProductRepository products;
    private ProductInventoryRepository inventory;
    private OrderRepository orders;
    private OrderItemRepository orderItems;
    private OrderStatusHistoryRepository history;
    private OrderStockReservationService reservations;
    private OrderCreationService service;

    @BeforeEach
    void setup() {
        customers = mock(CustomerRepository.class);
        products = mock(ProductRepository.class);
        inventory = mock(ProductInventoryRepository.class);
        orders = mock(OrderRepository.class);
        orderItems = mock(OrderItemRepository.class);
        history = mock(OrderStatusHistoryRepository.class);
        reservations = mock(OrderStockReservationService.class);

        service = new OrderCreationService(
            customers, products, inventory, orders,
            orderItems, history, reservations
        );
    }

    private InternalOrderCreateRequest request(String paymentMethod) {
        return new InternalOrderCreateRequest(
            "customer@example.invalid",
            "checkout-test-key-001",
            "STORE_PICKUP",
            paymentMethod,
            "Test Customer",
            "9876543210",
            null, null, null, null, null, null, null,
            List.of(new CheckoutItemRequest(10L, 1))
        );
    }

    private Customer customer(long id) {
        Customer result = mock(Customer.class);
        when(result.getId()).thenReturn(id);
        when(result.isActive()).thenReturn(true);
        return result;
    }

    private Order existingOrder() {
        Order order = mock(Order.class);

        when(order.getId()).thenReturn(100L);
        when(order.getOrderNumber()).thenReturn("KCMART-TEST-100");
        when(order.getCustomerId()).thenReturn(1L);
        when(order.getFulfillmentType()).thenReturn("STORE_PICKUP");
        when(order.getPaymentMethod()).thenReturn("PAY_AT_STORE");
        when(order.getCustomerName()).thenReturn("Test Customer");
        when(order.getCustomerPhone()).thenReturn("9876543210");
        when(order.getPaymentStatus()).thenReturn("PENDING");
        when(order.getOrderStatus()).thenReturn("PENDING");
        when(order.getSubtotal()).thenReturn(new BigDecimal("100.00"));
        when(order.getDeliveryCharge()).thenReturn(new BigDecimal("0.00"));
        when(order.getTotalAmount()).thenReturn(new BigDecimal("100.00"));

        return order;
    }

    private void stubExistingOrder(long customerId) {
        Customer customer = customer(customerId);
        Order existing = existingOrder();

        when(customers.findByEmailIgnoreCaseForUpdate(
            "customer@example.invalid"
        )).thenReturn(Optional.of(customer));

        when(orders.findByIdempotencyKey(
            "checkout-test-key-001"
        )).thenReturn(Optional.of(existing));
    }

    @Test
    void matchingRequestReturnsExistingOrder() {
        stubExistingOrder(1L);

        OrderItem item = mock(OrderItem.class);
        when(item.getProductId()).thenReturn(10L);
        when(item.getQuantity()).thenReturn(1);

        when(orderItems.findByOrderIdOrderByIdAsc(100L))
            .thenReturn(new ArrayList<>(List.of(item)));

        OrderCreateResponse response =
            service.createStorePickupOrder(request("PAY_AT_STORE"));

        assertEquals(100L, response.orderId());
        assertTrue(response.idempotentReplay());

        verify(orders, never()).save(any(Order.class));
        verifyNoInteractions(reservations);
    }

    @Test
    void differentCustomerCannotReuseIdempotencyKey() {
        stubExistingOrder(2L);

        ResponseStatusException exception = assertThrows(
            ResponseStatusException.class,
            () -> service.createStorePickupOrder(
                request("PAY_AT_STORE")
            )
        );

        assertEquals(409, exception.getStatusCode().value());
        verify(orders, never()).save(any(Order.class));
    }

    @Test
    void changedPaymentMethodCannotReuseIdempotencyKey() {
        stubExistingOrder(1L);

        ResponseStatusException exception = assertThrows(
            ResponseStatusException.class,
            () -> service.createStorePickupOrder(
                request("ONLINE")
            )
        );

        assertEquals(409, exception.getStatusCode().value());
        verify(orders, never()).save(any(Order.class));
    }

    private InternalOrderCreateRequest deliveryRequest(
        String addressLine1
    ) {
        return new InternalOrderCreateRequest(
            "customer@example.invalid",
            "checkout-test-key-001",
            "DELIVERY",
            "ONLINE",
            "Test Customer",
            "9876543210",
            addressLine1,
            null,
            null,
            "Hyderabad",
            "Telangana",
            "500001",
            "India",
            List.of(new CheckoutItemRequest(10L, 1))
        );
    }

    private void stubExistingDeliveryOrder() {
        Customer customer = customer(1L);
        Order existing = existingOrder();

        when(existing.getFulfillmentType()).thenReturn("DELIVERY");
        when(existing.getPaymentMethod()).thenReturn("ONLINE");
        when(existing.getAddressLine1()).thenReturn("Original Address");
        when(existing.getCity()).thenReturn("Hyderabad");
        when(existing.getState()).thenReturn("Telangana");
        when(existing.getPostalCode()).thenReturn("500001");
        when(existing.getCountry()).thenReturn("India");

        when(customers.findByEmailIgnoreCaseForUpdate(
            "customer@example.invalid"
        )).thenReturn(Optional.of(customer));

        when(orders.findByIdempotencyKey(
            "checkout-test-key-001"
        )).thenReturn(Optional.of(existing));
    }

    @Test
    void changedCartQuantityRejectsReplay() {
        stubExistingOrder(1L);

        OrderItem item = mock(OrderItem.class);
        when(item.getProductId()).thenReturn(10L);
        when(item.getQuantity()).thenReturn(2);

        when(orderItems.findByOrderIdOrderByIdAsc(100L))
            .thenReturn(new ArrayList<>(List.of(item)));

        ResponseStatusException exception = assertThrows(
            ResponseStatusException.class,
            () -> service.createStorePickupOrder(
                request("PAY_AT_STORE")
            )
        );

        assertEquals(409, exception.getStatusCode().value());
        verify(orders, never()).save(any(Order.class));
        verifyNoInteractions(reservations);
    }

    @Test
    void changedDeliveryAddressRejectsReplay() {
        stubExistingDeliveryOrder();

        ResponseStatusException exception = assertThrows(
            ResponseStatusException.class,
            () -> service.createStorePickupOrder(
                deliveryRequest("Changed Address")
            )
        );

        assertEquals(409, exception.getStatusCode().value());
        verify(orders, never()).save(any(Order.class));
        verifyNoInteractions(reservations);
    }

    @Test
    void matchingDeliveryRequestReturnsExistingOrder() {
        stubExistingDeliveryOrder();

        OrderItem item = mock(OrderItem.class);
        when(item.getProductId()).thenReturn(10L);
        when(item.getQuantity()).thenReturn(1);

        when(orderItems.findByOrderIdOrderByIdAsc(100L))
            .thenReturn(new ArrayList<>(List.of(item)));

        OrderCreateResponse response =
            service.createStorePickupOrder(
                deliveryRequest("Original Address")
            );

        assertEquals(100L, response.orderId());
        assertTrue(response.idempotentReplay());

        verify(orders, never()).save(any(Order.class));
        verifyNoInteractions(reservations);
    }

    private void stubNewCheckoutCustomer() {
        Customer customer = customer(1L);

        when(customers.findByEmailIgnoreCaseForUpdate(
            "customer@example.invalid"
        )).thenReturn(Optional.of(customer));

        when(orders.findByIdempotencyKey(
            "checkout-test-key-001"
        )).thenReturn(Optional.empty());
    }

    private in.kcmart.backend.catalog.entity.Product product(
        boolean active
    ) {
        in.kcmart.backend.catalog.entity.Product product =
            mock(in.kcmart.backend.catalog.entity.Product.class);

        when(product.getId()).thenReturn(10L);
        when(product.isActive()).thenReturn(active);

        if (active) {
            when(product.getPrice())
                .thenReturn(new BigDecimal("100.00"));
            when(product.getName()).thenReturn("Test Product");
        }

        return product;
    }

    @Test
    void insufficientStoreStockRejectsNewOrder() {
        stubNewCheckoutCustomer();

        var testProduct = product(true);
        var stock = mock(
            in.kcmart.backend.catalog.entity.ProductInventory.class
        );

        when(products.findById(10L))
            .thenReturn(Optional.of(testProduct));

        when(inventory.findByProductIdForUpdate(10L))
            .thenReturn(Optional.of(stock));

        when(stock.getStoreStock()).thenReturn(0);

        ResponseStatusException exception = assertThrows(
            ResponseStatusException.class,
            () -> service.createStorePickupOrder(
                request("PAY_AT_STORE")
            )
        );

        assertEquals(409, exception.getStatusCode().value());
        verify(orders, never()).save(any(Order.class));
        verifyNoInteractions(reservations);
    }

    @Test
    void insufficientOnlineStockRejectsNewOrder() {
        stubNewCheckoutCustomer();

        var testProduct = product(true);
        var stock = mock(
            in.kcmart.backend.catalog.entity.ProductInventory.class
        );

        when(products.findById(10L))
            .thenReturn(Optional.of(testProduct));

        when(inventory.findByProductIdForUpdate(10L))
            .thenReturn(Optional.of(stock));

        when(stock.getOnlineStock()).thenReturn(5);

        when(reservations.getActiveReservedQuantity(10L))
            .thenReturn(5);

        ResponseStatusException exception = assertThrows(
            ResponseStatusException.class,
            () -> service.createStorePickupOrder(
                deliveryRequest("Original Address")
            )
        );

        assertEquals(409, exception.getStatusCode().value());
        verify(orders, never()).save(any(Order.class));
        verify(reservations).getActiveReservedQuantity(10L);
        verify(reservations, never()).reserveDeliveryStock(
            anyLong(), anyLong(), anyInt()
        );
    }

    @Test
    void inactiveProductRejectsNewOrder() {
        stubNewCheckoutCustomer();

        var testProduct = product(false);

        when(products.findById(10L))
            .thenReturn(Optional.of(testProduct));

        ResponseStatusException exception = assertThrows(
            ResponseStatusException.class,
            () -> service.createStorePickupOrder(
                request("PAY_AT_STORE")
            )
        );

        assertEquals(409, exception.getStatusCode().value());
        verifyNoInteractions(inventory);
        verify(orders, never()).save(any(Order.class));
        verifyNoInteractions(reservations);
    }
}
