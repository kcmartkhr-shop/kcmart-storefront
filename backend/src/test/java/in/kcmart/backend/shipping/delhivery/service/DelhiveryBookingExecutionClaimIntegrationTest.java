package in.kcmart.backend.shipping.delhivery.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = {
    "delhivery.enabled=false",
    "delhivery.shipment-creation-enabled=false",
    "spring.task.scheduling.enabled=false"
})
@ActiveProfiles("test")
class DelhiveryBookingExecutionClaimIntegrationTest {

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    PlatformTransactionManager transactionManager;

    @Autowired
    DelhiveryBookingExecutionClaimService executionClaim;

    @Test
    void firstClaimCommitsAndSecondClaimIsRejected() {

        String database = jdbc.queryForObject(
            "SELECT current_database()", String.class
        );

        assertEquals("kcmart_test", database,
            "Refusing to run against non-test database");

        String suffix = UUID.randomUUID().toString().replace("-", "");
        String email = "claim-" + suffix + "@example.invalid";
        String orderNumber = "CL-" + suffix;

        TransactionTemplate tx = new TransactionTemplate(transactionManager);

        Long orderId = tx.execute(status -> {

            Long customerId = jdbc.queryForObject(
                """
                INSERT INTO customers (
                    provider, provider_subject, email
                )
                VALUES ('TEST', ?, ?)
                RETURNING id
                """,
                Long.class,
                "claim-" + suffix,
                email
            );

            Long createdOrderId = jdbc.queryForObject(
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
                    'Claim Test', ?, '9999999999',
                    'Test Address', 'Bhubaneswar', 'Odisha',
                    '751001', 'India',
                    100, 0, 100,
                    'ONLINE', 'PAID', 'PACKED'
                )
                RETURNING id
                """,
                Long.class,
                orderNumber,
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
                createdOrderId
            );

            return createdOrderId;
        });

        assertNotNull(orderId);

        try {
            executionClaim.claim(orderId);

            String firstStatus = jdbc.queryForObject(
                """
                SELECT attempt_status
                FROM delhivery_shipment_booking_attempts
                WHERE order_id = ?
                """,
                String.class,
                orderId
            );

            assertEquals("PROCESSING", firstStatus);

            assertThrows(
                IllegalStateException.class,
                () -> executionClaim.claim(orderId)
            );

            String secondStatus = jdbc.queryForObject(
                """
                SELECT attempt_status
                FROM delhivery_shipment_booking_attempts
                WHERE order_id = ?
                """,
                String.class,
                orderId
            );

            assertEquals("PROCESSING", secondStatus);
        }
        finally {
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
                    "claim-" + suffix
                );
            });
        }

        Long remaining = jdbc.queryForObject(
            """
            SELECT COUNT(*)
            FROM delhivery_shipment_booking_attempts
            WHERE order_id = ?
            """,
            Long.class,
            orderId
        );

        assertEquals(0L, remaining);
    }
}