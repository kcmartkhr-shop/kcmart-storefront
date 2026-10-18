package in.kcmart.backend.order.controller;

import in.kcmart.backend.order.dto.admin.AdminOrderCancellationRequest;
import in.kcmart.backend.order.dto.admin.AdminOrderInvoiceResponse;
import in.kcmart.backend.order.dto.admin.AdminOrderNotificationResponse;
import in.kcmart.backend.order.dto.admin.AdminOrderResponse;
import in.kcmart.backend.order.dto.admin.AdminOrderStatusUpdateRequest;
import in.kcmart.backend.order.dto.admin.AdminPaymentReceivedRequest;
import in.kcmart.backend.order.dto.admin.AdminOrderShipmentResponse;
import in.kcmart.backend.order.dto.admin.AdminOrderShipmentUpsertRequest;
import in.kcmart.backend.order.entity.Order;
import in.kcmart.backend.order.entity.OrderInvoice;
import in.kcmart.backend.order.service.AdminOrderNotificationQueryService;
import in.kcmart.backend.order.service.AdminOrderQueryService;
import in.kcmart.backend.order.service.OrderCancellationService;
import in.kcmart.backend.order.service.OrderInvoiceService;
import in.kcmart.backend.order.service.OrderLifecycleService;
import in.kcmart.backend.order.service.OrderPaymentService;
import in.kcmart.backend.order.service.OrderShipmentService;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/orders")
public class AdminOrderController {

    private final AdminOrderQueryService queryService;
    private final AdminOrderNotificationQueryService notificationQueryService;
    private final OrderLifecycleService lifecycleService;
    private final OrderCancellationService cancellationService;
    private final OrderPaymentService paymentService;
    private final OrderInvoiceService invoiceService;
    private final OrderShipmentService shipmentService;

    public AdminOrderController(
            AdminOrderQueryService queryService,
            AdminOrderNotificationQueryService notificationQueryService,
            OrderLifecycleService lifecycleService,
            OrderCancellationService cancellationService,
            OrderPaymentService paymentService,
            OrderInvoiceService invoiceService,
            OrderShipmentService shipmentService) {

        this.queryService = queryService;
        this.notificationQueryService = notificationQueryService;
        this.lifecycleService = lifecycleService;
        this.cancellationService = cancellationService;
        this.paymentService = paymentService;
        this.invoiceService = invoiceService;
        this.shipmentService = shipmentService;
    }

    @GetMapping
    public List<AdminOrderResponse> list() {
        return queryService.listAll();
    }

    @GetMapping("/{id}")
    public AdminOrderResponse get(
            @PathVariable Long id) {

        return queryService.getById(id);
    }

    @GetMapping("/{id}/notifications")
    public List<AdminOrderNotificationResponse> notifications(
            @PathVariable Long id) {

        return notificationQueryService.getByOrderId(id);
    }

    @PatchMapping("/{id}/status")
    public AdminOrderResponse updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody AdminOrderStatusUpdateRequest request,
            Principal principal) {

        String adminUsername =
            principal != null
                ? principal.getName()
                : "admin";

        Order updated = lifecycleService.transition(
            id,
            request.status(),
            adminUsername,
            request.note()
        );

        return queryService.getById(updated.getId());
    }

    @PatchMapping("/{id}/cancel")
    public AdminOrderResponse cancelOrder(
            @PathVariable Long id,
            @RequestBody(required = false)
                AdminOrderCancellationRequest request,
            Principal principal) {

        String adminUsername =
            principal != null
                ? principal.getName()
                : "admin";

        String note =
            request != null
                ? request.note()
                : null;

        Order updated =
            cancellationService.cancelPendingStorePickup(
                id,
                adminUsername,
                note
            );

        return queryService.getById(
            updated.getId()
        );
    }

    @PatchMapping("/{id}/payment/received")
    public AdminOrderResponse markPaymentReceived(
            @PathVariable Long id,
            @Valid @RequestBody AdminPaymentReceivedRequest request,
            Principal principal) {

        String adminUsername =
            principal != null
                ? principal.getName()
                : "admin";

        Order updated = paymentService.markPayAtStoreReceived(
            id,
            adminUsername,
            request.note()
        );

        return queryService.getById(updated.getId());
    }

    @PostMapping(
        value = "/{id}/invoice",
        consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public AdminOrderInvoiceResponse uploadInvoice(
            @PathVariable Long id,
            @RequestPart("file") MultipartFile file,
            Principal principal) {

        String adminUsername =
            principal != null
                ? principal.getName()
                : "admin";

        OrderInvoice invoice = invoiceService.uploadInvoice(
            id,
            file,
            adminUsername
        );

        return toInvoiceResponse(invoice);
    }

    @GetMapping("/{id}/invoice")
    public AdminOrderInvoiceResponse getInvoice(
            @PathVariable Long id) {

        return toInvoiceResponse(
            invoiceService.getByOrderId(id)
        );
    }

    @GetMapping("/{id}/shipment")
    public AdminOrderShipmentResponse getShipment(
            @PathVariable Long id) {

        return shipmentService.getByOrderId(id);
    }

    @PutMapping("/{id}/shipment")
    public AdminOrderShipmentResponse upsertShipment(
            @PathVariable Long id,
            @RequestBody AdminOrderShipmentUpsertRequest request,
            Principal principal) {

        String adminUsername =
            principal != null
                ? principal.getName()
                : "admin";

        return shipmentService.upsert(
            id,
            request,
            adminUsername
        );
    }
    private AdminOrderInvoiceResponse toInvoiceResponse(
            OrderInvoice invoice) {

        return new AdminOrderInvoiceResponse(
            invoice.getId(),
            invoice.getOrderId(),
            invoice.getInvoiceNumber(),
            invoice.getOriginalFilename(),
            invoice.getContentType(),
            invoice.getFileSize(),
            invoice.getUploadedBy(),
            invoice.getCreatedAt(),
            invoice.getUpdatedAt()
        );
    }
}