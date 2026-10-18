package in.kcmart.backend.catalog.repository;

import in.kcmart.backend.catalog.entity.ProductImage;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProductImageRepository
        extends JpaRepository<ProductImage, Long> {

    List<ProductImage> findByProductIdOrderByDisplayOrderAscIdAsc(
            Long productId
    );

    long countByProductId(Long productId);

    Optional<ProductImage> findByIdAndProductId(
            Long id,
            Long productId
    );

    Optional<ProductImage> findByProductIdAndCoverTrue(
            Long productId
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        select image
        from ProductImage image
        where image.productId = :productId
        order by image.displayOrder asc, image.id asc
    """)
    List<ProductImage> findByProductIdForUpdate(
            @Param("productId") Long productId
    );
}