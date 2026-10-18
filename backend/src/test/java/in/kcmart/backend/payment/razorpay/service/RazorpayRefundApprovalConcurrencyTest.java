package in.kcmart.backend.payment.razorpay.service;

import in.kcmart.backend.payment.razorpay.entity.RazorpayRefundAttempt;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.UUID;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = {
    "spring.task.scheduling.enabled=false",
    "kcmart.reservation.expiry.enabled=false"
})
@ActiveProfiles("test")
class RazorpayRefundApprovalConcurrencyTest {

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    RazorpayRefundApprovalService approval;

    @Test
    void simultaneousApprovalsMustCreateOnlyOneRefund() throws Exception {

        String database = jdbc.queryForObject(
            "SELECT current_database()", String.class
        );

        assertEquals(
            "kcmart_refund_concurrency_test",
            database,
            "SAFETY STOP: Wrong database"
        );

        String suffix = UUID.randomUUID().toString().replace("-", "");
        String email = "concurrency-" + suffix + "@example.invalid";
        String razorpayOrder = "order_concurrent_" + suffix;
        String razorpayPayment = "pay_concurrent_" + suffix;
        String key = "refund-concurrent-" + suffix;

        Long customerId = jdbc.queryForObject(
            """
            INSERT INTO customers (provider, provider_subject, email)
            VALUES ('TEST', ?, ?)
            RETURNING id
            """,
            Long.class, suffix, email
        );

        Long orderId = jdbc.queryForObject(
            """
            INSERT INTO orders
                (order_number, customer_id, fulfillment_type,
                 customer_name, customer_email, customer_phone,
                 address_line1, city, state, postal_code, country,
                 subtotal, delivery_charge, total_amount,
                 payment_method, payment_status, order_status)
            VALUES (?, ?, 'DELIVERY',
                    'Concurrency Customer', ?, '9999999999',
                    'Test Address', 'Test City', 'Odisha', '751001', 'India',
                    100.00, 0.00, 100.00,
                    'ONLINE', 'FAILED', 'CANCELLED')
            RETURNING id
            """,
            Long.class,
            "CON-" + suffix.substring(0, 24),
            customerId,
            email
        );

        assertTrue(orderId > 10L);

        Long paymentAttemptId = jdbc.queryForObject(
            """
            INSERT INTO razorpay_payment_attempts
                (order_id, razorpay_order_id, amount_paise, currency, status)
            VALUES (?, ?, 10000, 'INR', 'CREATED')
            RETURNING id
            """,
            Long.class, orderId, razorpayOrder
        );

        Long reconciliationId = jdbc.queryForObject(
            """
            INSERT INTO razorpay_payment_reconciliations
                (order_id, payment_attempt_id, razorpay_order_id,
                 razorpay_payment_id, amount_paise, currency,
                 reason, status)
            VALUES (?, ?, ?, ?, 10000, 'INR',
                    'CAPTURED_AFTER_ORDER_CANCELLATION', 'CAPTURED_REVIEW')
            RETURNING id
            """,
            Long.class,
            orderId,
            paymentAttemptId,
            razorpayOrder,
            razorpayPayment
        );

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);

        try {
            Callable<Long> task = () -> {
                ready.countDown();

                if (!start.await(10, TimeUnit.SECONDS)) {
                    throw new IllegalStateException("Start signal timeout");
                }

                RazorpayRefundAttempt attempt = approval.approveFullRefund(
                    reconciliationId, key, "admin-concurrency-test"
                );

                return attempt.getId();
            };

            Future<Long> first = executor.submit(task);
            Future<Long> second = executor.submit(task);

            assertTrue(ready.await(10, TimeUnit.SECONDS));
            start.countDown();

            Long firstId = first.get(30, TimeUnit.SECONDS);
            Long secondId = second.get(30, TimeUnit.SECONDS);

            assertEquals(firstId, secondId);

            Integer attempts = jdbc.queryForObject(
                """
                SELECT COUNT(*)
                FROM razorpay_refund_attempts
                WHERE reconciliation_id = ?
                """,
                Integer.class,
                reconciliationId
            );

            Integer events = jdbc.queryForObject(
                """
                SELECT COUNT(*)
                FROM razorpay_refund_audit_events
                WHERE refund_attempt_id = ?
                """,
                Integer.class,
                firstId
            );

            assertEquals(1, attempts);
            assertEquals(1, events);

            assertEquals(
                "PENDING",
                jdbc.queryForObject(
                    "SELECT status FROM razorpay_refund_attempts WHERE id = ?",
                    String.class, firstId
                )
            );

            assertEquals(
                "REFUND_PENDING",
                jdbc.queryForObject(
                    "SELECT status FROM razorpay_payment_reconciliations WHERE id = ?",
                    String.class, reconciliationId
                )
            );

            System.out.println(
                "CONCURRENT REFUND VERIFIED: both requests returned same ID"
            );

            System.out.println(
                "ONE REFUND ATTEMPT VERIFIED: count=1"
            );

            System.out.println(
                "ONE AUDIT EVENT VERIFIED: count=1"
            );

        } finally {
            start.countDown();
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS));
        }
    }
}