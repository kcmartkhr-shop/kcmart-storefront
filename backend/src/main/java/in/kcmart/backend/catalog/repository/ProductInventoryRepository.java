package in.kcmart.backend.catalog.repository;

import in.kcmart.backend.catalog.entity.ProductInventory;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ProductInventoryRepository
        extends JpaRepository<ProductInventory, Long> {

    Optional<ProductInventory> findByProductId(
        Long productId
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        select inventory
        from ProductInventory inventory
        where inventory.productId = :productId
        """)
    Optional<ProductInventory> findByProductIdForUpdate(
        @Param("productId") Long productId
    );
}