package in.kcmart.backend.payment.razorpay.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.server.ResponseStatusException;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class RazorpayPaymentDatabaseIntegrationTest {

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private RazorpayPaymentFinalizationService finalizationService;

    private void verifyTestDatabase() throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            String database = jdbc.queryForObject(
                    "SELECT current_database()", String.class
            );

            assertEquals("kcmart_test", database,
                    "Refusing to modify a non-test database");

            assertTrue(
                    connection.getMetaData().getURL().contains("/kcmart_test"),
                    "Unexpected JDBC connection"
            );
        }
    }

    @Test
    void successfulPaymentIsIdempotent() throws Exception {
        verifyTestDatabase();

        String suffix = UUID.randomUUID().toString().replace("-", "");
        String razorpayOrderId = "order_it_" + suffix;
        String razorpayPaymentId = "pay_it_" + suffix;

        Long categoryId = null;
        Long productId = null;
        Long customerId = null;
        Long orderId = null;

        try {
            categoryId = jdbc.queryForObject(
                    "INSERT INTO categories (name, slug) VALUES (?, ?) RETURNING id",
                    Long.class, "IT Category", "it-category-" + suffix
            );

            productId = jdbc.queryForObject(
                    """
                    INSERT INTO products
                    (category_id, name, slug, sku, price)
                    VALUES (?, ?, ?, ?, 100.00)
                    RETURNING id
                    """,
                    Long.class,
                    categoryId,
                    "IT Product",
                    "it-product-" + suffix,
                    "IT-" + suffix
            );

            jdbc.update(
                    """
                    INSERT INTO product_inventory
                    (product_id, online_stock, store_stock)
                    VALUES (?, 10, 0)
                    """,
                    productId
            );

            customerId = jdbc.queryForObject(
                    """
                    INSERT INTO customers
                    (provider, provider_subject, email)
                    VALUES ('TEST', ?, ?)
                    RETURNING id
                    """,
                    Long.class,
                    suffix,
                    "it-" + suffix + "@example.invalid"
            );

            orderId = jdbc.queryForObject(
                    """
                    INSERT INTO orders
                    (order_number, customer_id, fulfillment_type,
                     customer_name, customer_email, customer_phone,
                     address_line1, city, state, postal_code, country,
                     subtotal, delivery_charge, total_amount,
                     payment_method, payment_status, order_status)
                    VALUES (?, ?, 'DELIVERY',
                            'IT Customer', ?, '9999999999',
                            'Test Address', 'Test City', 'Odisha', '751001', 'India',
                            200.00, 0.00, 200.00,
                            'ONLINE', 'PENDING', 'PENDING')
                    RETURNING id
                    """,
                    Long.class,
                    "IT-" + suffix,
                    customerId,
                    "it-" + suffix + "@example.invalid"
            );

            jdbc.update(
                    """
                    INSERT INTO order_items
                    (order_id, product_id, product_name, sku,
                     unit_price, quantity, line_total)
                    VALUES (?, ?, 'IT Product', ?, 100.00, 2, 200.00)
                    """,
                    orderId, productId, "IT-" + suffix
            );

            jdbc.update(
                    """
                    INSERT INTO order_stock_reservations
                    (order_id, product_id, quantity, status, expires_at)
                    VALUES (?, ?, 2, 'ACTIVE', now() + interval '10 minutes')
                    """,
                    orderId, productId
            );

            jdbc.update(
                    """
                    INSERT INTO razorpay_payment_attempts
                    (order_id, razorpay_order_id, amount_paise, currency, status)
                    VALUES (?, ?, 20000, 'INR', 'CREATED')
                    """,
                    orderId, razorpayOrderId
            );

            var first = finalizationService.finalizeCapturedPayment(
                    orderId, razorpayOrderId, razorpayPaymentId,
                    20000L, "INR", true
            );

            assertFalse(first.alreadyFinalized());

            var second = finalizationService.finalizeCapturedPayment(
                    orderId, razorpayOrderId, razorpayPaymentId,
                    20000L, "INR", true
            );

            assertTrue(second.alreadyFinalized());

            assertEquals("PAID", jdbc.queryForObject(
                    "SELECT payment_status FROM orders WHERE id = ?",
                    String.class, orderId
            ));

            assertEquals("CAPTURED", jdbc.queryForObject(
                    """
                    SELECT status FROM razorpay_payment_attempts
                    WHERE order_id = ?
                    """,
                    String.class, orderId
            ));

            assertEquals("CONSUMED", jdbc.queryForObject(
                    """
                    SELECT status FROM order_stock_reservations
                    WHERE order_id = ?
                    """,
                    String.class, orderId
            ));

            assertEquals(8, jdbc.queryForObject(
                    """
                    SELECT online_stock FROM product_inventory
                    WHERE product_id = ?
                    """,
                    Integer.class, productId
            ));

            assertEquals(1, jdbc.queryForObject(
                    """
                    SELECT COUNT(*) FROM order_payment_history
                    WHERE order_id = ? AND new_status = 'PAID'
                    """,
                    Integer.class, orderId
            ));

        } finally {
            verifyTestDatabase();

            if (orderId != null) {
                jdbc.update("DELETE FROM order_payment_history WHERE order_id = ?", orderId);
                jdbc.update("DELETE FROM razorpay_payment_attempts WHERE order_id = ?", orderId);
                jdbc.update("DELETE FROM order_stock_reservations WHERE order_id = ?", orderId);
                jdbc.update("DELETE FROM order_items WHERE order_id = ?", orderId);
                jdbc.update("DELETE FROM orders WHERE id = ?", orderId);
            }

            if (productId != null) {
                jdbc.update("DELETE FROM product_inventory WHERE product_id = ?", productId);
                jdbc.update("DELETE FROM products WHERE id = ?", productId);
            }

            if (categoryId != null) {
                jdbc.update("DELETE FROM categories WHERE id = ?", categoryId);
            }

            if (customerId != null) {
                jdbc.update("DELETE FROM customers WHERE id = ?", customerId);
            }
        }
    }

    @Test
    void expiredReservationRollsBackPayment() throws Exception {
        verifyTestDatabase();

        String suffix = UUID.randomUUID().toString().replace("-", "");
        String razorpayOrderId = "order_it_" + suffix;
        String razorpayPaymentId = "pay_it_" + suffix;
        String email = "it-" + suffix + "@example.invalid";

        Long categoryId = null;
        Long productId = null;
        Long customerId = null;
        Long orderId = null;

        try {
            categoryId = jdbc.queryForObject(
                    "INSERT INTO categories (name, slug) VALUES (?, ?) RETURNING id",
                    Long.class, "IT Rollback Category", "it-category-" + suffix
            );

            productId = jdbc.queryForObject(
                    """
                    INSERT INTO products
                    (category_id, name, slug, sku, price)
                    VALUES (?, ?, ?, ?, 100.00)
                    RETURNING id
                    """,
                    Long.class, categoryId, "IT Rollback Product",
                    "it-product-" + suffix, "IT-" + suffix
            );

            jdbc.update(
                    """
                    INSERT INTO product_inventory
                    (product_id, online_stock, store_stock)
                    VALUES (?, 10, 0)
                    """,
                    productId
            );

            customerId = jdbc.queryForObject(
                    """
                    INSERT INTO customers
                    (provider, provider_subject, email)
                    VALUES ('TEST', ?, ?)
                    RETURNING id
                    """,
                    Long.class, suffix, email
            );

            orderId = jdbc.queryForObject(
                    """
                    INSERT INTO orders
                    (order_number, customer_id, fulfillment_type,
                     customer_name, customer_email, customer_phone,
                     address_line1, city, state, postal_code, country,
                     subtotal, delivery_charge, total_amount,
                     payment_method, payment_status, order_status)
                    VALUES (?, ?, 'DELIVERY',
                            'IT Customer', ?, '9999999999',
                            'Test Address', 'Test City', 'Odisha', '751001', 'India',
                            200.00, 0.00, 200.00,
                            'ONLINE', 'PENDING', 'PENDING')
                    RETURNING id
                    """,
                    Long.class, "IT-" + suffix, customerId, email
            );

            jdbc.update(
                    """
                    INSERT INTO order_items
                    (order_id, product_id, product_name, sku,
                     unit_price, quantity, line_total)
                    VALUES (?, ?, 'IT Rollback Product', ?, 100.00, 2, 200.00)
                    """,
                    orderId, productId, "IT-" + suffix
            );

            jdbc.update(
                    """
                    INSERT INTO order_stock_reservations
                    (order_id, product_id, quantity, status, expires_at)
                    VALUES (?, ?, 2, 'ACTIVE', now() - interval '1 minute')
                    """,
                    orderId, productId
            );

            jdbc.update(
                    """
                    INSERT INTO razorpay_payment_attempts
                    (order_id, razorpay_order_id, amount_paise, currency, status)
                    VALUES (?, ?, 20000, 'INR', 'CREATED')
                    """,
                    orderId, razorpayOrderId
            );

            Long finalOrderId = orderId;

            ResponseStatusException exception = assertThrows(
                    ResponseStatusException.class,
                    () -> finalizationService.finalizeCapturedPayment(
                            finalOrderId, razorpayOrderId, razorpayPaymentId,
                            20000L, "INR", true
                    )
            );

            assertEquals(409, exception.getStatusCode().value());

            assertEquals("PENDING", jdbc.queryForObject(
                    "SELECT payment_status FROM orders WHERE id = ?",
                    String.class, orderId
            ));

            assertEquals("CREATED", jdbc.queryForObject(
                    """
                    SELECT status FROM razorpay_payment_attempts
                    WHERE order_id = ?
                    """,
                    String.class, orderId
            ));

            assertEquals("ACTIVE", jdbc.queryForObject(
                    """
                    SELECT status FROM order_stock_reservations
                    WHERE order_id = ?
                    """,
                    String.class, orderId
            ));

            assertEquals(10, jdbc.queryForObject(
                    """
                    SELECT online_stock FROM product_inventory
                    WHERE product_id = ?
                    """,
                    Integer.class, productId
            ));

            assertEquals(0, jdbc.queryForObject(
                    """
                    SELECT COUNT(*) FROM order_payment_history
                    WHERE order_id = ? AND new_status = 'PAID'
                    """,
                    Integer.class, orderId
            ));

        } finally {
            verifyTestDatabase();

            if (orderId != null) {
                jdbc.update("DELETE FROM order_payment_history WHERE order_id = ?", orderId);
                jdbc.update("DELETE FROM razorpay_payment_attempts WHERE order_id = ?", orderId);
                jdbc.update("DELETE FROM order_stock_reservations WHERE order_id = ?", orderId);
                jdbc.update("DELETE FROM order_items WHERE order_id = ?", orderId);
                jdbc.update("DELETE FROM orders WHERE id = ?", orderId);
            }

            if (productId != null) {
                jdbc.update("DELETE FROM product_inventory WHERE product_id = ?", productId);
                jdbc.update("DELETE FROM products WHERE id = ?", productId);
            }

            if (categoryId != null) {
                jdbc.update("DELETE FROM categories WHERE id = ?", categoryId);
            }

            if (customerId != null) {
                jdbc.update("DELETE FROM customers WHERE id = ?", customerId);
            }
        }
    }
}