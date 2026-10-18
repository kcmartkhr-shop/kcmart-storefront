package in.kcmart.backend.payment.razorpay.service;

import in.kcmart.backend.order.reservation.service.DeliveryReservationExpiryProcessor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = {
    "spring.task.scheduling.enabled=false",
    "kcmart.reservation.expiry.enabled=false"
})
@ActiveProfiles("test")
class PaymentExpiryRaceIntegrationTest {

    @Autowired JdbcTemplate jdbc;
    @Autowired RazorpayPaymentFinalizationService finalizer;
    @Autowired DeliveryReservationExpiryProcessor expiry;

    @Test
    void validReservationPaymentMustSurviveConcurrentExpiry() throws Exception {
        runRace(false);
    }

    @Test
    void expiredReservationMustNeverBecomePaid() throws Exception {
        runRace(true);
    }

    private void verifyTestDatabase() {
        assertEquals(
            "kcmart_test",
            jdbc.queryForObject("SELECT current_database()", String.class),
            "Refusing to modify a non-test database"
        );
    }

    private void runRace(boolean expired) throws Exception {
        verifyTestDatabase();

        String suffix = UUID.randomUUID().toString().replace("-", "");
        String razorpayOrderId = "order_race_" + suffix;
        String razorpayPaymentId = "pay_race_" + suffix;
        String email = "race-" + suffix + "@example.invalid";

        Long categoryId = null;
        Long productId = null;
        Long customerId = null;
        Long orderId = null;

        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {
            categoryId = jdbc.queryForObject(
                "INSERT INTO categories (name, slug) VALUES (?, ?) RETURNING id",
                Long.class, "Race Category", "race-category-" + suffix
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
                "Race Product",
                "race-product-" + suffix,
                "RACE-" + suffix
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
                        'Race Customer', ?, '9999999999',
                        'Test Address', 'Test City', 'Odisha', '751001', 'India',
                        100.00, 0.00, 100.00,
                        'ONLINE', 'PENDING', 'PENDING')
                RETURNING id
                """,
                Long.class, "RACE-" + suffix, customerId, email
            );

            jdbc.update(
                """
                INSERT INTO order_items
                    (order_id, product_id, product_name, sku,
                     unit_price, quantity, line_total)
                VALUES (?, ?, 'Race Product', ?, 100.00, 1, 100.00)
                """,
                orderId, productId, "RACE-" + suffix
            );

            jdbc.update(
                """
                INSERT INTO order_stock_reservations
                    (order_id, product_id, quantity, status, expires_at)
                VALUES (?, ?, 1, 'ACTIVE',
                        NOW() + (? * INTERVAL '1 minute'))
                """,
                orderId, productId, expired ? -1 : 10
            );

            jdbc.update(
                """
                INSERT INTO razorpay_payment_attempts
                    (order_id, razorpay_order_id, amount_paise, currency, status)
                VALUES (?, ?, 10000, 'INR', 'CREATED')
                """,
                orderId, razorpayOrderId
            );

            final Long id = orderId;
            final CountDownLatch ready = new CountDownLatch(2);
            final CountDownLatch start = new CountDownLatch(1);
            final AtomicReference<Throwable> paymentError = new AtomicReference<>();
            final AtomicReference<Throwable> expiryError = new AtomicReference<>();

            Future<?> paymentTask = executor.submit(() -> {
                ready.countDown();
                try {
                    if (!start.await(10, TimeUnit.SECONDS)) {
                        throw new IllegalStateException("Start timeout");
                    }
                    finalizer.finalizeCapturedPayment(
                        id, razorpayOrderId, razorpayPaymentId,
                        10000L, "INR", true
                    );
                } catch (Throwable ex) {
                    paymentError.set(ex);
                }
            });

            Future<?> expiryTask = executor.submit(() -> {
                ready.countDown();
                try {
                    if (!start.await(10, TimeUnit.SECONDS)) {
                        throw new IllegalStateException("Start timeout");
                    }
                    expiry.expireOneOrder(id);
                } catch (Throwable ex) {
                    expiryError.set(ex);
                }
            });

            assertTrue(ready.await(10, TimeUnit.SECONDS));
            start.countDown();

            paymentTask.get(30, TimeUnit.SECONDS);
            expiryTask.get(30, TimeUnit.SECONDS);

            assertNull(expiryError.get(), "Expiry task failed: " + expiryError.get());

            String paymentStatus = field(
                "SELECT payment_status FROM orders WHERE id = ?", id
            );
            String orderStatus = field(
                "SELECT order_status FROM orders WHERE id = ?", id
            );
            String reservationStatus = field(
                "SELECT status FROM order_stock_reservations WHERE order_id = ?", id
            );
            String attemptStatus = field(
                "SELECT status FROM razorpay_payment_attempts WHERE order_id = ?", id
            );

            int stock = jdbc.queryForObject(
                "SELECT online_stock FROM product_inventory WHERE product_id = ?",
                Integer.class, productId
            );

            int paidHistory = jdbc.queryForObject(
                """
                SELECT COUNT(*) FROM order_payment_history
                WHERE order_id = ? AND new_status = 'PAID'
                """,
                Integer.class, id
            );

            if (expired) {
                assertInstanceOf(
                    ResponseStatusException.class,
                    paymentError.get(),
                    "Expired payment must be rejected"
                );
                assertEquals(
                    409,
                    ((ResponseStatusException) paymentError.get())
                        .getStatusCode().value()
                );
                assertEquals("FAILED", paymentStatus);
                assertEquals("CANCELLED", orderStatus);
                assertEquals("RELEASED", reservationStatus);
                assertEquals("CREATED", attemptStatus);
                assertEquals(1, stock);
                assertEquals(0, paidHistory);

                System.out.println(
                    "EXPIRED RACE VERIFIED: cancelled/released, " +
                    "payment rejected 409, stock=1"
                );
            } else {
                assertNull(
                    paymentError.get(),
                    "Valid payment failed: " + paymentError.get()
                );
                assertEquals("PAID", paymentStatus);
                assertNotEquals("CANCELLED", orderStatus);
                assertEquals("CONSUMED", reservationStatus);
                assertEquals("CAPTURED", attemptStatus);
                assertEquals(0, stock);
                assertEquals(1, paidHistory);

                System.out.println(
                    "VALID RACE VERIFIED: paid/consumed, " +
                    "expiry did not cancel, stock=0"
                );
            }

        } finally {
            executor.shutdownNow();
            verifyTestDatabase();

            if (orderId != null) {
                jdbc.update(
                    "DELETE FROM order_payment_history WHERE order_id = ?",
                    orderId
                );
                jdbc.update(
                    "DELETE FROM order_status_history WHERE order_id = ?",
                    orderId
                );
                jdbc.update(
                    "DELETE FROM razorpay_payment_attempts WHERE order_id = ?",
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
                jdbc.update("DELETE FROM orders WHERE id = ?", orderId);
            }

            if (productId != null) {
                jdbc.update(
                    "DELETE FROM product_inventory WHERE product_id = ?",
                    productId
                );
                jdbc.update("DELETE FROM products WHERE id = ?", productId);
            }

            if (customerId != null) {
                jdbc.update("DELETE FROM customers WHERE id = ?", customerId);
            }

            if (categoryId != null) {
                jdbc.update("DELETE FROM categories WHERE id = ?", categoryId);
            }
        }
    }

    private String field(String sql, Long id) {
        return jdbc.queryForObject(sql, String.class, id);
    }
}