package in.kcmart.backend.order.service;

import in.kcmart.backend.customer.entity.Customer;
import in.kcmart.backend.customer.repository.CustomerRepository;
import in.kcmart.backend.order.dto.customer.CustomerOrderDetailResponse;
import in.kcmart.backend.order.dto.customer.CustomerOrderItemResponse;
import in.kcmart.backend.order.dto.customer.CustomerOrderInvoiceResponse;
import in.kcmart.backend.order.dto.customer.CustomerOrderShipmentResponse;
import in.kcmart.backend.order.dto.customer.CustomerOrderStatusResponse;
import in.kcmart.backend.order.dto.customer.CustomerOrderSummaryResponse;
import in.kcmart.backend.order.entity.Order;
import in.kcmart.backend.order.entity.OrderItem;
import in.kcmart.backend.order.entity.OrderShipment;
import in.kcmart.backend.order.entity.OrderStatusHistory;
import in.kcmart.backend.order.repository.OrderInvoiceRepository;
import in.kcmart.backend.order.repository.OrderItemRepository;
import in.kcmart.backend.order.repository.OrderRepository;
import in.kcmart.backend.order.repository.OrderShipmentRepository;
import in.kcmart.backend.order.repository.OrderStatusHistoryRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class CustomerOrderQueryService {

    private final CustomerRepository customers;
    private final OrderRepository orders;
    private final OrderInvoiceRepository invoices;
    private final OrderItemRepository orderItems;
    private final OrderStatusHistoryRepository orderHistory;
    private final OrderShipmentRepository shipments;

    public CustomerOrderQueryService(
            CustomerRepository customers,
            OrderRepository orders,
            OrderItemRepository orderItems,
            OrderStatusHistoryRepository orderHistory,
            OrderShipmentRepository shipments,
            OrderInvoiceRepository invoices) {

        this.customers = customers;
        this.orders = orders;
        this.orderItems = orderItems;
        this.orderHistory = orderHistory;
        this.shipments = shipments;
        this.invoices = invoices;
    }

    @Transactional(readOnly = true)
    public List<CustomerOrderSummaryResponse>
            findOrdersForTrustedEmail(String trustedEmail) {

        Customer customer =
            resolveActiveCustomer(trustedEmail);

        return orders
            .findByCustomerIdOrderByCreatedAtDesc(
                customer.getId()
            )
            .stream()
            .map(this::toSummary)
            .toList();
    }

    @Transactional(readOnly = true)
    public CustomerOrderDetailResponse
            findOrderForTrustedEmail(
                String trustedEmail,
                Long orderId) {

        if (orderId == null || orderId <= 0) {
            throw new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Order not found."
            );
        }

        Customer customer =
            resolveActiveCustomer(trustedEmail);

        Order order = orders
            .findById(orderId)
            .orElseThrow(() ->
                orderNotFound()
            );

        /*
         * SECURITY:
         *
         * Never trust the order ID by itself.
         *
         * The order must belong to the customer
         * resolved from the trusted server-side
         * Auth.js email.
         *
         * Return the same 404 for a missing order
         * and an order belonging to someone else.
         * This avoids leaking order existence.
         */
        if (!customer.getId().equals(
                order.getCustomerId())) {

            throw orderNotFound();
        }

        List<CustomerOrderItemResponse> items =
            orderItems
                .findByOrderIdOrderByIdAsc(
                    order.getId()
                )
                .stream()
                .map(this::toItem)
                .toList();

        List<CustomerOrderStatusResponse>
            statusHistory =
                orderHistory
                    .findByOrderIdOrderByCreatedAtAscIdAsc(
                        order.getId()
                    )
                    .stream()
                    .map(this::toStatus)
                    .toList();

        CustomerOrderShipmentResponse shipment =
            shipments
                .findByOrderId(order.getId())
                .map(this::toShipment)
                .orElse(null);

        CustomerOrderInvoiceResponse invoice =
            invoices
                .findByOrderId(order.getId())
                .map(this::toInvoice)
                .orElse(null);

        return new CustomerOrderDetailResponse(
            order.getId(),
            order.getOrderNumber(),
            order.getFulfillmentType(),

            order.getCustomerName(),
            order.getCustomerEmail(),
            order.getCustomerPhone(),

            order.getAddressLine1(),
            order.getAddressLine2(),
            order.getLandmark(),
            order.getCity(),
            order.getState(),
            order.getPostalCode(),
            order.getCountry(),

            order.getSubtotal(),
            order.getDeliveryCharge(),
            order.getTotalAmount(),

            order.getPaymentMethod(),
            order.getPaymentStatus(),
            order.getOrderStatus(),

            order.getCreatedAt(),
            order.getUpdatedAt(),

            items,
            statusHistory,
            shipment,
            invoice
        );
    }

    private Customer resolveActiveCustomer(
            String trustedEmail) {

        String email =
            normalizeEmail(trustedEmail);

        Customer customer = customers
            .findByEmailIgnoreCase(email)
            .orElseThrow(() ->
                new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Customer account not found."
                )
            );

        if (!customer.isActive()) {
            throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "Customer account is inactive."
            );
        }

        return customer;
    }

    private CustomerOrderSummaryResponse
            toSummary(Order order) {

        return new CustomerOrderSummaryResponse(
            order.getId(),
            order.getOrderNumber(),
            order.getFulfillmentType(),
            order.getTotalAmount(),
            order.getPaymentMethod(),
            order.getPaymentStatus(),
            order.getOrderStatus(),
            order.getCreatedAt(),
            order.getUpdatedAt()
        );
    }

    private CustomerOrderItemResponse
            toItem(OrderItem item) {

        return new CustomerOrderItemResponse(
            item.getProductId(),
            item.getProductName(),
            item.getSku(),
            item.getUnitPrice(),
            item.getQuantity(),
            item.getLineTotal()
        );
    }

    private CustomerOrderStatusResponse
            toStatus(OrderStatusHistory history) {

        return new CustomerOrderStatusResponse(
            history.getNewStatus(),
            history.getCreatedAt()
        );
    }

    private CustomerOrderShipmentResponse
            toShipment(OrderShipment shipment) {

        return new CustomerOrderShipmentResponse(
            shipment.getCarrierName(),
            shipment.getTrackingNumber(),
            safeTrackingUrl(
                shipment.getTrackingUrl()
            ),
            shipment.getShipmentStatus(),
            shipment.getShippedAt(),
            shipment.getOutForDeliveryAt(),
            shipment.getDeliveredAt()
        );
    }

    private CustomerOrderInvoiceResponse
            toInvoice(in.kcmart.backend.order.entity.OrderInvoice invoice) {

        return new CustomerOrderInvoiceResponse(
            true,
            invoice.getInvoiceNumber(),
            invoice.getOriginalFilename(),
            invoice.getCreatedAt()
        );
    }

    private String safeTrackingUrl(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        String url = value.trim();

        if (url.startsWith("https://")
                || url.startsWith("http://")) {
            return url;
        }

        return null;
    }

    private ResponseStatusException
            orderNotFound() {

        return new ResponseStatusException(
            HttpStatus.NOT_FOUND,
            "Order not found."
        );
    }

    private String normalizeEmail(String value) {
        if (value == null || value.isBlank()) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Trusted customer email is required."
            );
        }

        String email =
            value.trim().toLowerCase();

        if (email.length() > 320) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Trusted customer email is invalid."
            );
        }

        return email;
    }
}