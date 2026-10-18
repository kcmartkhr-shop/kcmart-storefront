package in.kcmart.backend.catalog.repository;

import in.kcmart.backend.catalog.entity.Product;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {

    List<Product> findByActiveTrueOrderByIdDesc();

    List<Product> findByCategoryIdAndActiveTrueOrderByIdDesc(Long categoryId);

    Optional<Product> findBySlugAndActiveTrue(String slug);

    long countByCategoryId(Long categoryId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Product p where p.id = :id")
    Optional<Product> findByIdForUpdate(@Param("id") Long id);
}