package in.kcmart.backend.order.service;

import in.kcmart.backend.order.dto.CheckoutItemRequest;
import in.kcmart.backend.order.dto.InternalOrderCreateRequest;
import in.kcmart.backend.order.dto.OrderCreateResponse;
import in.kcmart.backend.order.reservation.service.DeliveryReservationExpiryProcessor;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = {
    "spring.task.scheduling.enabled=false",
    "kcmart.reservation.expiry.enabled=false"
})
@ActiveProfiles("test")
class ReservationExpiryIntegrationTest {

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    OrderCreationService checkout;

    @Autowired
    DeliveryReservationExpiryProcessor expiryProcessor;

    @Test
    void expiredCheckoutIsCancelledAndStockCanBeReservedAgain() {
        String suffix = UUID.randomUUID().toString().replace("-", "");
        String slug = "expiry-test-" + suffix;
        String keyA = "expiry-a-" + suffix;
        String keyB = "expiry-b-" + suffix;
        String emailA = "expiry-a-" + suffix + "@example.invalid";
        String emailB = "expiry-b-" + suffix + "@example.invalid";

        Long categoryId = null;
        Long productId = null;
        Long customerA = null;
        Long customerB = null;

        try {
            categoryId = jdbc.queryForObject(
                "INSERT INTO categories (name, slug) VALUES (?, ?) RETURNING id",
                Long.class, "Expiry Integration Test", slug
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
                "Expiry Integration Product",
                slug,
                "EXP-" + suffix,
                new BigDecimal("100.00")
            );

            jdbc.update(
                """
                INSERT INTO product_inventory
                    (product_id, online_stock, store_stock)
                VALUES (?, 1, 0)
                """,
                productId
            );

            customerA = createCustomer(
                "expiry-subject-a-" + suffix, emailA
            );

            customerB = createCustomer(
                "expiry-subject-b-" + suffix, emailB
            );

            OrderCreateResponse first = checkout.createStorePickupOrder(
                request(emailA, keyA, productId)
            );

            assertNotNull(first.orderId());

            assertEquals(
                1,
                activeReservations(productId),
                "First checkout must reserve one unit"
            );

            assertEquals(
                1,
                onlineStock(productId),
                "Unpaid checkout must not deduct physical online stock"
            );

            jdbc.update(
                """
                UPDATE order_stock_reservations
                SET expires_at = NOW() - INTERVAL '1 minute'
                WHERE order_id = ?
                  AND product_id = ?
                  AND status = 'ACTIVE'
                """,
                first.orderId(), productId
            );

            expiryProcessor.expireOneOrder(first.orderId());

            assertEquals(
                "CANCELLED",
                orderField(first.orderId(), "order_status")
            );

            assertEquals(
                "FAILED",
                orderField(first.orderId(), "payment_status")
            );

            assertEquals(
                "RELEASED",
                reservationStatus(first.orderId(), productId)
            );

            assertEquals(
                0,
                activeReservations(productId),
                "Expired reservation must stop blocking stock"
            );

            assertEquals(
                1,
                onlineStock(productId),
                "Expiry must not deduct online stock"
            );

            OrderCreateResponse second = checkout.createStorePickupOrder(
                request(emailB, keyB, productId)
            );

            assertNotNull(second.orderId());
            assertNotEquals(first.orderId(), second.orderId());

            assertEquals(
                "ACTIVE",
                reservationStatus(second.orderId(), productId)
            );

            assertEquals(
                1,
                activeReservations(productId),
                "Only the new customer's reservation must be active"
            );

            assertEquals(
                2,
                jdbc.queryForObject(
                    """
                    SELECT COUNT(*)
                    FROM order_stock_reservations
                    WHERE product_id = ?
                    """,
                    Integer.class,
                    productId
                )
            );

            assertEquals(
                1,
                onlineStock(productId),
                "No physical stock deduction before payment"
            );

            expiryProcessor.expireOneOrder(first.orderId());

            assertEquals(
                "PENDING",
                orderField(second.orderId(), "order_status"),
                "Reprocessing old expiry must not cancel the new order"
            );

            System.out.println(
                "EXPIRY VERIFIED: first=CANCELLED/RELEASED " +
                "second=PENDING/ACTIVE activeReservations=1 " +
                "onlineStock=1"
            );

        } finally {
            if (productId != null) {
                jdbc.update(
                    "DELETE FROM order_stock_reservations WHERE product_id = ?",
                    productId
                );
            }

            jdbc.update(
                """
                DELETE FROM order_status_history
                WHERE order_id IN (
                    SELECT id FROM orders
                    WHERE idempotency_key IN (?, ?)
                )
                """,
                keyA, keyB
            );

            jdbc.update(
                """
                DELETE FROM order_items
                WHERE order_id IN (
                    SELECT id FROM orders
                    WHERE idempotency_key IN (?, ?)
                )
                """,
                keyA, keyB
            );

            jdbc.update(
                "DELETE FROM orders WHERE idempotency_key IN (?, ?)",
                keyA, keyB
            );

            if (productId != null) {
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

    private Long createCustomer(String subject, String email) {
        return jdbc.queryForObject(
            """
            INSERT INTO customers
                (provider, provider_subject, email, name)
            VALUES ('TEST', ?, ?, 'Expiry Test Customer')
            RETURNING id
            """,
            Long.class, subject, email
        );
    }

    private InternalOrderCreateRequest request(
        String email, String key, Long productId
    ) {
        return new InternalOrderCreateRequest(
            email,
            key,
            "DELIVERY",
            "ONLINE",
            "Expiry Test Customer",
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
    }

    private int activeReservations(Long productId) {
        return jdbc.queryForObject(
            """
            SELECT COUNT(*)
            FROM order_stock_reservations
            WHERE product_id = ?
              AND status = 'ACTIVE'
              AND expires_at > NOW()
            """,
            Integer.class, productId
        );
    }

    private int onlineStock(Long productId) {
        return jdbc.queryForObject(
            "SELECT online_stock FROM product_inventory WHERE product_id = ?",
            Integer.class, productId
        );
    }

    private String reservationStatus(Long orderId, Long productId) {
        return jdbc.queryForObject(
            """
            SELECT status
            FROM order_stock_reservations
            WHERE order_id = ? AND product_id = ?
            """,
            String.class, orderId, productId
        );
    }

    private String orderField(Long orderId, String column) {
        if (!List.of("order_status", "payment_status").contains(column)) {
            throw new IllegalArgumentException("Unsupported column");
        }

        return jdbc.queryForObject(
            "SELECT " + column + " FROM orders WHERE id = ?",
            String.class, orderId
        );
    }
}