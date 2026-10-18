package in.kcmart.backend.order.repository;

import in.kcmart.backend.order.entity.OrderInvoice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OrderInvoiceRepository
        extends JpaRepository<OrderInvoice, Long> {

    Optional<OrderInvoice> findByOrderId(Long orderId);

    boolean existsByOrderId(Long orderId);

    boolean existsByInvoiceNumber(String invoiceNumber);
}