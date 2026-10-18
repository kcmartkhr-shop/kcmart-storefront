package in.kcmart.backend.shipping.delhivery.repository;

import in.kcmart.backend.shipping.delhivery.entity.DelhiveryShipmentPackage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DelhiveryShipmentPackageRepository
        extends JpaRepository<DelhiveryShipmentPackage, Long> {

    Optional<DelhiveryShipmentPackage> findByOrderId(Long orderId);

    boolean existsByOrderId(Long orderId);
}