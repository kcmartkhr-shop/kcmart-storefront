package in.kcmart.backend.catalog.repository;

import in.kcmart.backend.catalog.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    List<Category> findByActiveTrueOrderByNameAsc();

    List<Category> findAllByOrderByNameAsc();

    boolean existsBySlugIgnoreCase(String slug);

    boolean existsBySlugIgnoreCaseAndIdNot(
        String slug,
        Long id
    );

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(
        String name,
        Long id
    );
}