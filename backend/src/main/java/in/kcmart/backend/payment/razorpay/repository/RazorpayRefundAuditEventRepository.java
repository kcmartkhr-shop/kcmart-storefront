package in.kcmart.backend.payment.razorpay.repository;

import in.kcmart.backend.payment.razorpay.entity.RazorpayRefundAuditEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RazorpayRefundAuditEventRepository
        extends JpaRepository<RazorpayRefundAuditEvent, Long> {
}