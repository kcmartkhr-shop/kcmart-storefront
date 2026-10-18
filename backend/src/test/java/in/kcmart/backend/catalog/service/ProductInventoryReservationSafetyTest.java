package in.kcmart.backend.catalog.service;

import in.kcmart.backend.catalog.dto.UpdateInventoryRequest;
import in.kcmart.backend.catalog.entity.Product;
import in.kcmart.backend.catalog.entity.ProductInventory;
import in.kcmart.backend.catalog.repository.CategoryRepository;
import in.kcmart.backend.catalog.repository.ProductInventoryRepository;
import in.kcmart.backend.catalog.repository.ProductRepository;
import in.kcmart.backend.order.reservation.repository.OrderStockReservationRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ProductInventoryReservationSafetyTest {

    private ProductRepository products;
    private ProductInventoryRepository inventory;
    private OrderStockReservationRepository reservations;
    private ProductManagementService service;

    @BeforeEach
    void setup() {
        products = mock(ProductRepository.class);
        inventory = mock(ProductInventoryRepository.class);
        reservations = mock(OrderStockReservationRepository.class);

        service = new ProductManagementService(
                reservations,
                products,
                mock(CategoryRepository.class),
                inventory
        );

        Product product = mock(Product.class);
        when(products.findById(1L)).thenReturn(Optional.of(product));

        ProductInventory stock = new ProductInventory();
        stock.setProductId(1L);
        stock.setOnlineStock(10);
        stock.setStoreStock(20);

        when(inventory.findByProductIdForUpdate(1L))
                .thenReturn(Optional.of(stock));

        when(reservations.sumActiveQuantity(eq(1L), any(OffsetDateTime.class)))
                .thenReturn(7);
    }

    @Test
    void rejectsStockBelowActiveReservations() {
        UpdateInventoryRequest request =
                new UpdateInventoryRequest(3, 20, 10, 20);

        ResponseStatusException error = assertThrows(
                ResponseStatusException.class,
                () -> service.updateInventory(1L, request)
        );

        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());

        verify(inventory, never()).saveAndFlush(any());
    }

    @Test
    void permitsStockEqualToActiveReservations() {
        UpdateInventoryRequest request =
                new UpdateInventoryRequest(7, 20, 10, 20);

        assertDoesNotThrow(() -> service.updateInventory(1L, request));

        verify(inventory).saveAndFlush(argThat(stock ->
                stock.getOnlineStock() == 7 &&
                stock.getStoreStock() == 20
        ));
    }

    @Test
    void storeStockCanChangeIndependently() {
        UpdateInventoryRequest request =
                new UpdateInventoryRequest(10, 5, 10, 20);

        assertDoesNotThrow(() -> service.updateInventory(1L, request));

        verify(inventory).saveAndFlush(argThat(stock ->
                stock.getOnlineStock() == 10 &&
                stock.getStoreStock() == 5
        ));
    }
}