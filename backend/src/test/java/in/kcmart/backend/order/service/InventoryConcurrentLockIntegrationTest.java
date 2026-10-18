package in.kcmart.backend.order.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.UUID;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = {
    "spring.task.scheduling.enabled=false",
    "delhivery.enabled=false",
    "delhivery.shipment-creation-enabled=false",
    "delhivery.label-retrieval-enabled=false"
})
@ActiveProfiles("test")
class InventoryConcurrentLockIntegrationTest {

    @Autowired
    DataSource dataSource;

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void concurrentInventoryLocksPreventStaleStockRead()
            throws Exception {

        assertEquals(
            "kcmart_test",
            jdbc.queryForObject(
                "SELECT current_database()", String.class
            ),
            "Refusing non-test database"
        );

        String suffix = UUID.randomUUID()
            .toString().replace("-", "");

        Long categoryId = null;
        Long productId = null;

        ExecutorService executor =
            Executors.newSingleThreadExecutor();

        try {
            categoryId = jdbc.queryForObject(
                """
                INSERT INTO categories (name, slug)
                VALUES (?, ?) RETURNING id
                """,
                Long.class,
                "Concurrency Test",
                "concurrency-category-" + suffix
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
                "Concurrency Product",
                "concurrency-product-" + suffix,
                "CON-" + suffix
            );

            jdbc.update(
                """
                INSERT INTO product_inventory
                (product_id, online_stock, store_stock)
                VALUES (?, 1, 0)
                """,
                productId
            );

            final Long id = productId;
            CountDownLatch started = new CountDownLatch(1);

            try (Connection first = dataSource.getConnection()) {

                assertTrue(
                    first.getMetaData().getURL()
                        .contains("/kcmart_test")
                );

                first.setAutoCommit(false);

                try {
                    try (PreparedStatement statement =
                             first.prepareStatement(
                                 """
                                 SELECT online_stock
                                 FROM product_inventory
                                 WHERE product_id = ?
                                 FOR UPDATE
                                 """
                             )) {

                        statement.setLong(1, id);

                        try (ResultSet result =
                                 statement.executeQuery()) {

                            assertTrue(result.next());
                            assertEquals(1, result.getInt(1));
                        }
                    }

                    Future<Integer> second = executor.submit(() -> {
                        try (Connection connection =
                                 dataSource.getConnection()) {

                            connection.setAutoCommit(false);

                            try {
                                started.countDown();

                                try (PreparedStatement statement =
                                         connection.prepareStatement(
                                             """
                                             SELECT online_stock
                                             FROM product_inventory
                                             WHERE product_id = ?
                                             FOR UPDATE
                                             """
                                         )) {

                                    statement.setLong(1, id);

                                    try (ResultSet result =
                                             statement.executeQuery()) {

                                        assertTrue(result.next());

                                        int stock = result.getInt(1);
                                        connection.commit();

                                        return stock;
                                    }
                                }
                            }
                            catch (Exception error) {
                                connection.rollback();
                                throw error;
                            }
                        }
                    });

                    assertTrue(
                        started.await(5, TimeUnit.SECONDS)
                    );

                    assertThrows(
                        TimeoutException.class,
                        () -> second.get(
                            350, TimeUnit.MILLISECONDS
                        )
                    );

                    try (PreparedStatement update =
                             first.prepareStatement(
                                 """
                                 UPDATE product_inventory
                                 SET online_stock = 0
                                 WHERE product_id = ?
                                 """
                             )) {

                        update.setLong(1, id);
                        assertEquals(1, update.executeUpdate());
                    }

                    first.commit();

                    assertEquals(
                        0,
                        second.get(10, TimeUnit.SECONDS)
                    );
                }
                catch (Exception error) {
                    first.rollback();
                    throw error;
                }
            }

            assertEquals(
                0,
                jdbc.queryForObject(
                    """
                    SELECT online_stock
                    FROM product_inventory
                    WHERE product_id = ?
                    """,
                    Integer.class,
                    productId
                )
            );
        }
        finally {
            executor.shutdownNow();

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

            if (categoryId != null) {
                jdbc.update(
                    "DELETE FROM categories WHERE id = ?",
                    categoryId
                );
            }
        }
    }
}