package in.kcmart.backend.order.service;

import in.kcmart.backend.order.entity.Order;
import in.kcmart.backend.order.entity.OrderInvoice;
import in.kcmart.backend.order.repository.OrderInvoiceRepository;
import in.kcmart.backend.order.repository.OrderRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Service
public class OrderInvoiceService {

    private final OrderRepository orders;
    private final OrderInvoiceRepository invoices;
    private final InvoiceStorageService storage;

    public OrderInvoiceService(
            OrderRepository orders,
            OrderInvoiceRepository invoices,
            InvoiceStorageService storage) {

        this.orders = orders;
        this.invoices = invoices;
        this.storage = storage;
    }

    @Transactional
    public OrderInvoice uploadInvoice(
            Long orderId,
            MultipartFile file,
            String actorIdentifier) {

        Order order = orders.findByIdForUpdate(orderId)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Order not found."
            ));

        String fulfillmentType = normalize(order.getFulfillmentType());
        String paymentMethod = normalize(order.getPaymentMethod());
        String paymentStatus = normalize(order.getPaymentStatus());
        String orderStatus = normalize(order.getOrderStatus());

        if (!"PAID".equals(paymentStatus)) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Payment must be PAID before the invoice can be uploaded."
            );
        }

        if ("STORE_PICKUP".equals(fulfillmentType)) {

            if (!"READY_FOR_PICKUP".equals(orderStatus)) {
                throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Store pickup order must be READY_FOR_PICKUP before the invoice can be uploaded."
                );
            }

        } else if ("DELIVERY".equals(fulfillmentType)) {

            if (!"ONLINE".equals(paymentMethod)) {
                throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Delivery invoice requires ONLINE payment."
                );
            }

            if (!java.util.Set.of(
                    "PACKED",
                    "SHIPPED",
                    "OUT_FOR_DELIVERY",
                    "DELIVERED"
                ).contains(orderStatus)) {

                throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Delivery order must be PACKED or later before the invoice can be uploaded."
                );
            }

        } else {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Unsupported fulfillment type for invoice upload."
            );
        }

        if (invoices.existsByOrderId(orderId)) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "An invoice already exists for this order."
            );
        }

        InvoiceStorageService.StoredInvoice stored = storage.store(file);

        try {
            OrderInvoice invoice = new OrderInvoice();

            invoice.setOrderId(order.getId());
            invoice.setInvoiceNumber(generateInvoiceNumber());
            invoice.setStorageKey(stored.storageKey());
            invoice.setOriginalFilename(stored.originalFilename());
            invoice.setContentType(stored.contentType());
            invoice.setFileSize(stored.fileSize());
            invoice.setUploadedBy(normalizeActor(actorIdentifier));

            return invoices.saveAndFlush(invoice);
        }
        catch (DataIntegrityViolationException ex) {
            storage.deleteQuietly(stored.storageKey());

            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Invoice could not be created because it conflicts with an existing invoice."
            );
        }
        catch (RuntimeException ex) {
            storage.deleteQuietly(stored.storageKey());
            throw ex;
        }
    }

    @Transactional(readOnly = true)
    public OrderInvoice getByOrderId(Long orderId) {

        return invoices.findByOrderId(orderId)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Invoice not found."
            ));
    }

    private String generateInvoiceNumber() {
        return "KCM-INV-" +
            UUID.randomUUID()
                .toString()
                .replace("-", "")
                .toUpperCase();
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }

        return value.trim().toUpperCase();
    }

    private String normalizeActor(String actorIdentifier) {

        if (actorIdentifier == null || actorIdentifier.isBlank()) {
            return "admin";
        }

        String value = actorIdentifier.trim();

        if (value.length() > 320) {
            return value.substring(0, 320);
        }

        return value;
    }
}