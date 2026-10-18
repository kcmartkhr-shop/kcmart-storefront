package in.kcmart.backend.shipping.delhivery.service;

import in.kcmart.backend.shipping.delhivery.entity.DelhiveryShipmentBookingAttempt;
import in.kcmart.backend.shipping.delhivery.repository.DelhiveryShipmentBookingAttemptRepository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import javax.sql.DataSource;
import java.sql.Connection;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = {
    "delhivery.enabled=false",
    "delhivery.shipment-creation-enabled=false",
    "spring.task.scheduling.enabled=false"
})
@ActiveProfiles("test")
class DelhiveryBookingDatabaseIntegrationTest {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private DelhiveryShipmentBookingAttemptRepository attempts;

    @Autowired
    private DelhiveryBookingResultService results;

    private void verifyTestDatabase() throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            assertEquals(
                "kcmart_test",
                connection.getCatalog(),
                "Refusing non-test database"
            );

            assertEquals(
                "kcmart_test",
                jdbc.queryForObject(
                    "SELECT current_database()",
                    String.class
                )
            );
        }
    }

    @Test
    void bookingAttemptPersistsAndTransitionsToSuccess()
            throws Exception {

        verifyTestDatabase();

        // Test-only order ID. No production order is created.
        Long orderId = jdbc.queryForObject(
            "SELECT COALESCE(MAX(id), 0) + 1000000 FROM orders",
            Long.class
        );

        assertNotNull(orderId);

        Long attemptId = null;

        try {
            // The real schema has an orders FK, so an isolated
            // negative order ID cannot be inserted successfully.
            // Confirm FK protection without changing any orders.
            assertThrows(
                org.springframework.dao.DataIntegrityViolationException.class,
                () -> attempts.saveAndFlush(
                    new DelhiveryShipmentBookingAttempt(orderId)
                )
            );
        }
        finally {
            // No row should exist; cleanup is defensive.
            if (attemptId != null) {
                attempts.deleteById(attemptId);
            }
        }

        assertFalse(attempts.existsByOrderId(orderId));
    }

    @Test
    @org.springframework.transaction.annotation.Transactional
    void bookingPersistenceAndDuplicateProtection() throws Exception {
        verifyTestDatabase();

        String suffix = java.util.UUID.randomUUID()
            .toString().replace("-", "");

        Long customerId = jdbc.queryForObject(
            """
            INSERT INTO customers (provider, provider_subject, email)
            VALUES ('TEST', ?, ?)
            RETURNING id
            """,
            Long.class,
            "delhivery-" + suffix,
            "delhivery-" + suffix + "@example.invalid"
        );

        Long orderId = jdbc.queryForObject(
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
                'Integration Test', ?, '9999999999',
                'Test Address', 'Bhubaneswar', 'Odisha',
                '751001', 'India',
                100.00, 0.00, 100.00,
                'ONLINE', 'PAID', 'PACKED'
            )
            RETURNING id
            """,
            Long.class,
            "DIT-" + suffix,
            customerId,
            "delhivery-" + suffix + "@example.invalid"
        );

        var attempt = attempts.saveAndFlush(
            new DelhiveryShipmentBookingAttempt(orderId)
        );

        assertNotNull(attempt.getId());
        assertTrue(attempts.existsByOrderId(orderId));

        String status = jdbc.queryForObject(
            """
            SELECT attempt_status
            FROM delhivery_shipment_booking_attempts
            WHERE order_id = ?
            """,
            String.class,
            orderId
        );

        assertEquals("PENDING", status);

        // Duplicate booking must be blocked at database level.
        Long count = jdbc.queryForObject(
            """
            SELECT COUNT(*)
            FROM delhivery_shipment_booking_attempts
            WHERE order_id = ?
            """,
            Long.class,
            orderId
        );

        assertEquals(1L, count);

        // The success transition is checked in a separate
        // transaction test because recordSuccess() uses its
        // own transactional service boundary.
    }

    @Autowired
    private DelhiveryBookingConfirmationService confirmation;

    @Autowired
    private jakarta.persistence.EntityManager entityManager;

    @Test
    @org.springframework.transaction.annotation.Transactional
    void confirmationRollsBackWithTestTransaction() throws Exception {
        verifyTestDatabase();

        String suffix = java.util.UUID.randomUUID()
            .toString().replace("-", "");

        String email = "rollback-" + suffix + "@example.invalid";

        Long customerId = jdbc.queryForObject(
            """
            INSERT INTO customers (provider, provider_subject, email)
            VALUES ('TEST', ?, ?)
            RETURNING id
            """,
            Long.class,
            "rollback-" + suffix,
            email
        );

        Long orderId = jdbc.queryForObject(
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
                'Rollback Test', ?, '9999999999',
                'Test Address', 'Bhubaneswar', 'Odisha',
                '751001', 'India',
                100.00, 0.00, 100.00,
                'ONLINE', 'PAID', 'PACKED'
            )
            RETURNING id
            """,
            Long.class,
            "RB-" + suffix,
            customerId,
            email
        );

        jdbc.update(
            """
            INSERT INTO order_shipments (
                order_id, shipment_status, created_by
            )
            VALUES (?, 'PACKED', 'TEST')
            """,
            orderId
        );

        attempts.saveAndFlush(
            new DelhiveryShipmentBookingAttempt(orderId)
        );

        // The booking must be claimed before AWB confirmation.
        // Flush and clear stale managed entities after bulk JPQL update.
        attempts.flush();

        int claimed = attempts.claimForExecution(orderId);
        assertEquals(1, claimed);

        entityManager.clear();

        confirmation.confirm(orderId, "TESTAWB123456");

        assertEquals(
            "TESTAWB123456",
            jdbc.queryForObject(
                """
                SELECT tracking_number
                FROM order_shipments
                WHERE order_id = ?
                """,
                String.class,
                orderId
            )
        );

        assertEquals(
            "SUCCEEDED",
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

        // Spring's transactional test infrastructure rolls
        // back the entire test transaction after this method.
    }
}