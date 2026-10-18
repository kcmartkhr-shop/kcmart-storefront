package in.kcmart.backend.shipping.delhivery.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.UUID;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = {
    "delhivery.enabled=false",
    "delhivery.shipment-creation-enabled=false",
    "spring.task.scheduling.enabled=false"
})
@ActiveProfiles("test")
class DelhiveryBookingConcurrentClaimIntegrationTest {

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    PlatformTransactionManager transactionManager;

    @Autowired
    DelhiveryBookingExecutionClaimService executionClaim;

    @Test
    void simultaneousClaimsAllowExactlyOneWinner() throws Exception {

        assertEquals(
            "kcmart_test",
            jdbc.queryForObject("SELECT current_database()", String.class),
            "Refusing to run against non-test database"
        );

        String suffix = UUID.randomUUID().toString().replace("-", "");
        String email = "concurrent-" + suffix + "@example.invalid";
        String subject = "concurrent-" + suffix;

        TransactionTemplate tx = new TransactionTemplate(transactionManager);

        Long orderId = tx.execute(status -> {

            Long customerId = jdbc.queryForObject(
                """
                INSERT INTO customers (provider, provider_subject, email)
                VALUES ('TEST', ?, ?)
                RETURNING id
                """,
                Long.class,
                subject,
                email
            );

            Long id = jdbc.queryForObject(
                """
                INSERT INTO orders (
                    order_number, customer_id, fulfillment_type,
                    customer_name, customer_email, customer_phone,
                    address_line1, city, state, postal_code, country,
                    subtotal, delivery_charge, total_amount,
                    payment_method, payment_status, order_status
                )
                VALUES (
                    ?, ?, 'DELIVERY',
                    'Concurrency Test', ?, '9999999999',
                    'Test Address', 'Bhubaneswar', 'Odisha',
                    '751001', 'India',
                    100, 0, 100,
                    'ONLINE', 'PAID', 'PACKED'
                )
                RETURNING id
                """,
                Long.class,
                "CC-" + suffix,
                customerId,
                email
            );

            jdbc.update(
                """
                INSERT INTO delhivery_shipment_booking_attempts (
                    order_id, attempt_status
                )
                VALUES (?, 'PENDING')
                """,
                id
            );

            return id;
        });

        assertNotNull(orderId);

        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);

        try {
            Callable<Boolean> task = () -> {
                ready.countDown();

                if (!start.await(10, TimeUnit.SECONDS)) {
                    throw new IllegalStateException("Start barrier timed out");
                }

                try {
                    executionClaim.claim(orderId);
                    return true;
                }
                catch (IllegalStateException rejected) {
                    return false;
                }
            };

            Future<Boolean> first = pool.submit(task);
            Future<Boolean> second = pool.submit(task);

            assertTrue(ready.await(10, TimeUnit.SECONDS),
                "Workers did not become ready");

            start.countDown();

            boolean firstWon = first.get(20, TimeUnit.SECONDS);
            boolean secondWon = second.get(20, TimeUnit.SECONDS);

            assertNotEquals(firstWon, secondWon,
                "Exactly one claim must succeed");

            assertEquals(
                "PROCESSING",
                jdbc.queryForObject(
                    """
                    SELECT attempt_status
                    FROM delhivery_shipment_booking_attempts
                    WHERE order_id = ?
                    """,
                    String.class,
                    orderId
                )
            );
        }
        finally {
            start.countDown();
            pool.shutdownNow();

            if (!pool.awaitTermination(30, TimeUnit.SECONDS)) {
                throw new IllegalStateException(
                    "Workers did not terminate; fixture cleanup skipped"
                );
            }

            tx.executeWithoutResult(status -> {
                jdbc.update(
                    "DELETE FROM delhivery_shipment_booking_attempts WHERE order_id = ?",
                    orderId
                );
                jdbc.update(
                    "DELETE FROM orders WHERE id = ?",
                    orderId
                );
                jdbc.update(
                    "DELETE FROM customers WHERE provider_subject = ?",
                    subject
                );
            });
        }

        assertEquals(
            0L,
            jdbc.queryForObject(
                """
                SELECT COUNT(*)
                FROM delhivery_shipment_booking_attempts
                WHERE order_id = ?
                """,
                Long.class,
                orderId
            )
        );
    }
}