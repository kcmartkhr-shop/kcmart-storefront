package in.kcmart.backend.customer.controller;

import in.kcmart.backend.customer.dto.CustomerResponse;
import in.kcmart.backend.customer.repository.CustomerRepository;
import in.kcmart.backend.order.repository.OrderRepository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/customers")
public class AdminCustomerController {

    private final CustomerRepository customers;
    private final OrderRepository orders;

    public AdminCustomerController(
        CustomerRepository customers,
        OrderRepository orders
    ) {
        this.customers = customers;
        this.orders = orders;
    }

    @GetMapping
    @Transactional(readOnly = true)
    public List<CustomerResponse> list() {
        return customers.findAll().stream()
            .map(CustomerResponse::from)
            .toList();
    }

    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    public CustomerResponse get(@PathVariable Long id) {
        return customers.findById(id)
            .map(CustomerResponse::from)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Customer not found"
            ));
    }

    @GetMapping("/{id}/orders")
    @Transactional(readOnly = true)
    public List<CustomerOrderRow> customerOrders(
        @PathVariable Long id
    ) {
        if (!customers.existsById(id)) {
            throw new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Customer not found"
            );
        }

        return orders.findByCustomerIdOrderByCreatedAtDesc(id)
            .stream()
            .map(order -> new CustomerOrderRow(
                order.getId(),
                order.getOrderNumber(),
                order.getTotalAmount(),
                order.getPaymentMethod(),
                order.getPaymentStatus(),
                order.getOrderStatus(),
                order.getCreatedAt()
            ))
            .toList();
    }

    public record CustomerOrderRow(
        Long id,
        String orderNumber,
        BigDecimal totalAmount,
        String paymentMethod,
        String paymentStatus,
        String orderStatus,
        OffsetDateTime createdAt
    ) {}
}