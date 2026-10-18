package in.kcmart.backend.order.service;

import in.kcmart.backend.order.dto.CheckoutItemRequest;
import in.kcmart.backend.order.dto.InternalOrderCreateRequest;
import in.kcmart.backend.order.dto.OrderCreateResponse;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class ConcurrentCheckoutIntegrationTest {

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private OrderCreationService orderCreationService;

    @Test
    void twoCustomersCannotReserveTheSameLastOnlineUnit() throws Exception {

        String suffix = UUID.randomUUID().toString().replace("-", "");
        String slug = "concurrency-" + suffix;
        String emailA = "buyer-a-" + suffix + "@example.invalid";
        String emailB = "buyer-b-" + suffix + "@example.invalid";

        Long categoryId = null;
        Long productId = null;
        Long customerA = null;
        Long customerB = null;

        ExecutorService pool = Executors.newFixedThreadPool(2);

        try {
            categoryId = jdbc.queryForObject(
                "INSERT INTO categories (name, slug) VALUES (?, ?) RETURNING id",
                Long.class,
                "Concurrency Test",
                slug
            );

            productId = jdbc.queryForObject(
                """
                INSERT INTO products
                    (category_id, name, slug, sku, price)
                VALUES (?, ?, ?, ?, ?)
                RETURNING id
                """,
                Long.class,
                categoryId,
                "Concurrent Checkout Test Product",
                slug,
                "TEST-" + suffix,
                new java.math.BigDecimal("100.00")
            );

            jdbc.update(
                """
                INSERT INTO product_inventory
                    (product_id, online_stock, store_stock)
                VALUES (?, 1, 0)
                """,
                productId
            );

            customerA = jdbc.queryForObject(
                """
                INSERT INTO customers
                    (provider, provider_subject, email, name)
                VALUES ('TEST', ?, ?, 'Buyer A')
                RETURNING id
                """,
                Long.class,
                "test-a-" + suffix,
                emailA
            );

            customerB = jdbc.queryForObject(
                """
                INSERT INTO customers
                    (provider, provider_subject, email, name)
                VALUES ('TEST', ?, ?, 'Buyer B')
                RETURNING id
                """,
                Long.class,
                "test-b-" + suffix,
                emailB
            );

            final Long testProductId = productId;
            final String keyA = "concurrent-a-" + suffix;
            final String keyB = "concurrent-b-" + suffix;

            CountDownLatch ready = new CountDownLatch(2);
            CountDownLatch start = new CountDownLatch(1);

            Callable<Integer> checkoutA = () ->
                attemptCheckout(emailA, keyA, testProductId, ready, start);

            Callable<Integer> checkoutB = () ->
                attemptCheckout(emailB, keyB, testProductId, ready, start);

            Future<Integer> first = pool.submit(checkoutA);
            Future<Integer> second = pool.submit(checkoutB);

            assertTrue(
                ready.await(10, TimeUnit.SECONDS),
                "Both checkout workers must become ready"
            );

            start.countDown();

            int resultA = first.get(30, TimeUnit.SECONDS);
            int resultB = second.get(30, TimeUnit.SECONDS);

            long successes = List.of(resultA, resultB).stream()
                .filter(status -> status == 200)
                .count();

            long conflicts = List.of(resultA, resultB).stream()
                .filter(status -> status == 409)
                .count();

            assertEquals(1, successes, "Exactly one checkout must succeed");
            assertEquals(1, conflicts, "Exactly one checkout must return 409");

            Integer orders = jdbc.queryForObject(
                """
                SELECT COUNT(*)
                FROM orders
                WHERE idempotency_key IN (?, ?)
                """,
                Integer.class,
                keyA,
                keyB
            );

            assertEquals(1, orders, "Exactly one order must exist");

            Integer activeReservations = jdbc.queryForObject(
                """
                SELECT COALESCE(SUM(quantity), 0)
                FROM order_stock_reservations
                WHERE product_id = ?
                  AND status = 'ACTIVE'
                  AND expires_at > NOW()
                """,
                Integer.class,
                productId
            );

            assertEquals(
                1,
                activeReservations,
                "Exactly one active stock unit must be reserved"
            );

            Integer onlineStock = jdbc.queryForObject(
                "SELECT online_stock FROM product_inventory WHERE product_id = ?",
                Integer.class,
                productId
            );

            assertEquals(
                1,
                onlineStock,
                "Unpaid delivery checkout must not permanently deduct stock"
            );

            assertEquals(
                0,
                onlineStock - activeReservations,
                "Effective available stock must be zero"
            );

            System.out.println(
                "CHECKOUT CONCURRENCY VERIFIED: " +
                "success=1 conflict=1 orders=1 " +
                "reserved=1 effectiveAvailable=0"
            );

        } finally {
            pool.shutdownNow();

            // Cleanup is restricted to IDs generated by this test.
            if (productId != null) {
                jdbc.update(
                    """
                    DELETE FROM order_stock_reservations
                    WHERE product_id = ?
                    """,
                    productId
                );

                jdbc.update(
                    """
                    DELETE FROM order_status_history
                    WHERE order_id IN (
                        SELECT id FROM orders
                        WHERE idempotency_key IN (?, ?)
                    )
                    """,
                    "concurrent-a-" + suffix,
                    "concurrent-b-" + suffix
                );

                jdbc.update(
                    """
                    DELETE FROM order_items
                    WHERE order_id IN (
                        SELECT id FROM orders
                        WHERE idempotency_key IN (?, ?)
                    )
                    """,
                    "concurrent-a-" + suffix,
                    "concurrent-b-" + suffix
                );

                jdbc.update(
                    """
                    DELETE FROM orders
                    WHERE idempotency_key IN (?, ?)
                    """,
                    "concurrent-a-" + suffix,
                    "concurrent-b-" + suffix
                );

                jdbc.update(
                    "DELETE FROM product_inventory WHERE product_id = ?",
                    productId
                );

                jdbc.update(
                    "DELETE FROM products WHERE id = ?",
                    productId
                );
            }

            if (customerA != null) {
                jdbc.update("DELETE FROM customers WHERE id = ?", customerA);
            }

            if (customerB != null) {
                jdbc.update("DELETE FROM customers WHERE id = ?", customerB);
            }

            if (categoryId != null) {
                jdbc.update("DELETE FROM categories WHERE id = ?", categoryId);
            }
        }
    }

    private int attemptCheckout(
        String email,
        String key,
        Long productId,
        CountDownLatch ready,
        CountDownLatch start
    ) throws Exception {

        ready.countDown();

        if (!start.await(10, TimeUnit.SECONDS)) {
            throw new IllegalStateException("Checkout start timed out");
        }

        InternalOrderCreateRequest request =
            new InternalOrderCreateRequest(
                email,
                key,
                "DELIVERY",
                "ONLINE",
                "Concurrency Test Buyer",
                "9876543210",
                "Test Address",
                null,
                null,
                "Hyderabad",
                "Telangana",
                "500001",
                "India",
                List.of(new CheckoutItemRequest(productId, 1))
            );

        try {
            OrderCreateResponse response =
                orderCreationService.createStorePickupOrder(request);

            assertNotNull(response.orderId());
            assertFalse(response.idempotentReplay());

            return 200;

        } catch (ResponseStatusException exception) {
            return exception.getStatusCode().value();
        }
    }
}