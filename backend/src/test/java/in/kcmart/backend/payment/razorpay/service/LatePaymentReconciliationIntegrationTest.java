package in.kcmart.backend.payment.razorpay.service;

import in.kcmart.backend.payment.razorpay.client.RazorpayClient.RazorpayPaymentResponse;
import in.kcmart.backend.payment.razorpay.entity.RazorpayPaymentReconciliation;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = {
    "spring.task.scheduling.enabled=false",
    "kcmart.reservation.expiry.enabled=false"
})
@ActiveProfiles("test")
class LatePaymentReconciliationIntegrationTest {

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    RazorpayPaymentReconciliationService reconciliation;

    @Test
    void expiredOrderCapturedPaymentMustBeRecordedExactlyOnce() {
        verifyDatabase();

        String suffix = UUID.randomUUID().toString().replace("-", "");
        String razorpayOrderId = "order_recon_" + suffix;
        String razorpayPaymentId = "pay_recon_" + suffix;
        String email = "recon-" + suffix + "@example.invalid";

        Long categoryId = null;
        Long productId = null;
        Long customerId = null;
        Long orderId = null;

        try {
            categoryId = jdbc.queryForObject(
                "INSERT INTO categories (name, slug) VALUES (?, ?) RETURNING id",
                Long.class,
                "Reconciliation Test",
                "reconciliation-" + suffix
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
                "Reconciliation Product",
                "recon-product-" + suffix,
                "RECON-" + suffix
            );

            jdbc.update(
                """
                INSERT INTO product_inventory
                    (product_id, online_stock, store_stock)
                VALUES (?, 1, 0)
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
                email
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
                        'Reconciliation Customer', ?, '9999999999',
                        'Test Address', 'Test City', 'Odisha', '751001', 'India',
                        100.00, 0.00, 100.00,
                        'ONLINE', 'PENDING', 'PENDING')
                RETURNING id
                """,
                Long.class,
                "RECON-" + suffix,
                customerId,
                email
            );

            assertTrue(
                orderId > 10L,
                "Test order must not be within frozen historical IDs"
            );

            jdbc.update(
                """
                INSERT INTO order_items
                    (order_id, product_id, product_name, sku,
                     unit_price, quantity, line_total)
                VALUES (?, ?, 'Reconciliation Product', ?, 100.00, 1, 100.00)
                """,
                orderId,
                productId,
                "RECON-" + suffix
            );

            jdbc.update(
                """
                INSERT INTO order_stock_reservations
                    (order_id, product_id, quantity, status, expires_at)
                VALUES (?, ?, 1, 'ACTIVE', NOW() - INTERVAL '10 minutes')
                """,
                orderId,
                productId
            );

            jdbc.update(
                """
                INSERT INTO razorpay_payment_attempts
                    (order_id, razorpay_order_id, amount_paise, currency, status)
                VALUES (?, ?, 10000, 'INR', 'CREATED')
                """,
                orderId,
                razorpayOrderId
            );

            RazorpayPaymentResponse payment =
                new RazorpayPaymentResponse(
                    razorpayPaymentId,
                    "payment",
                    10000L,
                    "INR",
                    "captured",
                    razorpayOrderId,
                    "upi",
                    true,
                    null,
                    null,
                    1000L
                );

            RazorpayPaymentReconciliation first =
                reconciliation.recordCapturedForCancelledOrder(
                    orderId, razorpayOrderId, payment
                );

            assertNotNull(first.getId());
            assertEquals("CAPTURED_REVIEW", first.getStatus());
            assertEquals(orderId, first.getOrderId());
            assertEquals(razorpayPaymentId, first.getRazorpayPaymentId());
            assertEquals(10000L, first.getAmountPaise());

            assertEquals(
                "CANCELLED",
                text("SELECT order_status FROM orders WHERE id = ?", orderId)
            );

            assertEquals(
                "FAILED",
                text("SELECT payment_status FROM orders WHERE id = ?", orderId)
            );

            assertEquals(
                "RELEASED",
                text(
                    "SELECT status FROM order_stock_reservations WHERE order_id = ?",
                    orderId
                )
            );

            assertEquals(
                1,
                number(
                    "SELECT online_stock FROM product_inventory WHERE product_id = ?",
                    productId
                )
            );

            assertEquals(
                1,
                number(
                    """
                    SELECT COUNT(*) FROM razorpay_payment_reconciliations
                    WHERE razorpay_payment_id = ?
                    """,
                    razorpayPaymentId
                )
            );

            assertEquals(
                1,
                number(
                    """
                    SELECT COUNT(*) FROM order_status_history
                    WHERE order_id = ?
                      AND new_status = 'CANCELLED'
                      AND actor_identifier = 'LATE_RAZORPAY_CAPTURE'
                    """,
                    orderId
                )
            );

            assertEquals(
                0,
                number(
                    """
                    SELECT COUNT(*) FROM order_payment_history
                    WHERE order_id = ? AND new_status = 'PAID'
                    """,
                    orderId
                )
            );

            RazorpayPaymentReconciliation second =
                reconciliation.recordCapturedForCancelledOrder(
                    orderId, razorpayOrderId, payment
                );

            assertEquals(first.getId(), second.getId());

            assertEquals(
                1,
                number(
                    """
                    SELECT COUNT(*) FROM razorpay_payment_reconciliations
                    WHERE razorpay_payment_id = ?
                    """,
                    razorpayPaymentId
                )
            );

            assertEquals(
                1,
                number(
                    """
                    SELECT COUNT(*) FROM order_status_history
                    WHERE order_id = ?
                      AND actor_identifier = 'LATE_RAZORPAY_CAPTURE'
                    """,
                    orderId
                )
            );

            assertEquals(
                "CREATED",
                text(
                    "SELECT status FROM razorpay_payment_attempts WHERE order_id = ?",
                    orderId
                )
            );

            assertEquals(
                1,
                number(
                    "SELECT online_stock FROM product_inventory WHERE product_id = ?",
                    productId
                )
            );

            System.out.println(
                "RECONCILIATION VERIFIED: CANCELLED/FAILED, " +
                "reservation RELEASED, stock=1, CAPTURED_REVIEW=1"
            );

            System.out.println(
                "DUPLICATE VERIFIED: same reconciliation ID, " +
                "one record, one cancellation history"
            );

        } finally {
            verifyDatabase();

            if (orderId != null) {
                jdbc.update(
                    "DELETE FROM razorpay_payment_reconciliations WHERE order_id = ?",
                    orderId
                );
                jdbc.update(
                    "DELETE FROM razorpay_webhook_events WHERE razorpay_order_id = ?",
                    razorpayOrderId
                );
                jdbc.update(
                    "DELETE FROM razorpay_payment_attempts WHERE order_id = ?",
                    orderId
                );
                jdbc.update(
                    "DELETE FROM order_payment_history WHERE order_id = ?",
                    orderId
                );
                jdbc.update(
                    "DELETE FROM order_status_history WHERE order_id = ?",
                    orderId
                );
                jdbc.update(
                    "DELETE FROM order_stock_reservations WHERE order_id = ?",
                    orderId
                );
                jdbc.update(
                    "DELETE FROM order_items WHERE order_id = ?",
                    orderId
                );
                jdbc.update(
                    "DELETE FROM orders WHERE id = ?",
                    orderId
                );
            }

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

            if (customerId != null) {
                jdbc.update(
                    "DELETE FROM customers WHERE id = ?",
                    customerId
                );
            }

            if (categoryId != null) {
                jdbc.update(
                    "DELETE FROM categories WHERE id = ?",
                    categoryId
                );
            }
        }
    }

    private void verifyDatabase() {
        assertEquals(
            "kcmart_test",
            jdbc.queryForObject("SELECT current_database()", String.class),
            "Safety stop: refusing non-test database"
        );
    }

    private String text(String sql, Object parameter) {
        return jdbc.queryForObject(sql, String.class, parameter);
    }

    private int number(String sql, Object parameter) {
        return jdbc.queryForObject(sql, Integer.class, parameter);
    }
}