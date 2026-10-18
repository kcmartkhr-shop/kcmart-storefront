package in.kcmart.backend.order.controller;

import in.kcmart.backend.order.dto.customer.CustomerOrderDetailResponse;
import in.kcmart.backend.order.dto.customer.CustomerOrderSummaryResponse;
import in.kcmart.backend.order.dto.customer.TrustedCustomerCancellationRequest;
import in.kcmart.backend.order.dto.customer.TrustedCustomerOrdersRequest;
import in.kcmart.backend.order.entity.Order;
import in.kcmart.backend.order.service.CustomerOrderQueryService;
import in.kcmart.backend.order.service.OrderCancellationService;
import in.kcmart.backend.order.service.InvoiceStorageService;
import in.kcmart.backend.order.repository.OrderInvoiceRepository;
import in.kcmart.backend.payment.razorpay.service.RazorpayPaymentAttemptService;
import in.kcmart.backend.payment.razorpay.dto.TrustedRazorpayPaymentVerificationRequest;
import in.kcmart.backend.payment.razorpay.service.RazorpayPaymentAttemptService.CheckoutOrder;
import in.kcmart.backend.payment.razorpay.service.RazorpayPaymentAttemptService.VerificationResult;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/internal/orders")
public class InternalCustomerOrderController {

    private final CustomerOrderQueryService
        orderQueryService;

    private final OrderCancellationService
        cancellationService;

    private final RazorpayPaymentAttemptService
        razorpayPaymentAttemptService;

    private final OrderInvoiceRepository
        invoiceRepository;

    private final InvoiceStorageService
        invoiceStorageService;

    public InternalCustomerOrderController(
            CustomerOrderQueryService orderQueryService,
            OrderCancellationService cancellationService,
            RazorpayPaymentAttemptService razorpayPaymentAttemptService,
            OrderInvoiceRepository invoiceRepository,
            InvoiceStorageService invoiceStorageService) {

        this.orderQueryService =
            orderQueryService;

        this.cancellationService =
            cancellationService;

        this.razorpayPaymentAttemptService =
            razorpayPaymentAttemptService;

        this.invoiceRepository =
            invoiceRepository;

        this.invoiceStorageService =
            invoiceStorageService;
    }

    @PostMapping("/customer")
    public ResponseEntity<
            List<CustomerOrderSummaryResponse>>
            findCustomerOrders(
                @Valid
                @RequestBody
                TrustedCustomerOrdersRequest request) {

        return ResponseEntity.ok(
            orderQueryService
                .findOrdersForTrustedEmail(
                    request.email()
                )
        );
    }

    @PostMapping("/customer/{orderId}")
    public ResponseEntity<
            CustomerOrderDetailResponse>
            findCustomerOrder(
                @PathVariable Long orderId,
                @Valid
                @RequestBody
                TrustedCustomerOrdersRequest request) {

        return ResponseEntity.ok(
            orderQueryService
                .findOrderForTrustedEmail(
                    request.email(),
                    orderId
                )
        );
    }

    @PatchMapping("/customer/{orderId}/cancel")
    public ResponseEntity<
            CustomerOrderDetailResponse>
            cancelCustomerOrder(
                @PathVariable Long orderId,
                @Valid
                @RequestBody
                TrustedCustomerCancellationRequest request) {

        Order cancelled =
            cancellationService
                .cancelPendingStorePickupForCustomer(
                    request.email(),
                    orderId,
                    request.note()
                );

        return ResponseEntity.ok(
            orderQueryService
                .findOrderForTrustedEmail(
                    request.email(),
                    cancelled.getId()
                )
        );
    }

    @PostMapping("/customer/{orderId}/invoice")
    public ResponseEntity<org.springframework.core.io.Resource>
            getCustomerInvoice(
                @PathVariable Long orderId,
                @Valid
                @RequestBody
                TrustedCustomerOrdersRequest request) {

        CustomerOrderDetailResponse order =
            orderQueryService.findOrderForTrustedEmail(
                request.email(),
                orderId
            );

        if (order.invoice() == null
                || !order.invoice().available()) {

            throw new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.NOT_FOUND,
                "Invoice not found."
            );
        }

        var invoice =
            invoiceRepository.findByOrderId(order.id())
                .orElseThrow(() ->
                    new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND,
                        "Invoice not found."
                    )
                );

        var resource =
            invoiceStorageService.load(
                invoice.getStorageKey()
            );

        return ResponseEntity.ok()
            .header(
                org.springframework.http.HttpHeaders.CONTENT_DISPOSITION,
                "inline; filename=\"" +
                    invoice.getOriginalFilename().replace("\"", "") +
                    "\""
            )
            .header(
                org.springframework.http.HttpHeaders.CONTENT_TYPE,
                "application/pdf"
            )
            .body(resource);
    }
    @PostMapping("/customer/{orderId}/payment/razorpay")
    public ResponseEntity<CheckoutOrder>
            createRazorpayPaymentOrder(
                @PathVariable Long orderId,
                @Valid
                @RequestBody
                TrustedCustomerOrdersRequest request) {

        return ResponseEntity.ok(
            razorpayPaymentAttemptService
                .createCheckoutOrder(
                    request.email(),
                    orderId
                )
        );
    }

    @PostMapping("/customer/{orderId}/payment/razorpay/verify")
    public ResponseEntity<VerificationResult>
            verifyRazorpayPayment(
                @PathVariable Long orderId,
                @Valid
                @RequestBody
                TrustedRazorpayPaymentVerificationRequest request) {

        return ResponseEntity.ok(
            razorpayPaymentAttemptService
                .verifyCheckoutSignature(
                    request.email(),
                    orderId,
                    request.razorpayOrderId(),
                    request.razorpayPaymentId(),
                    request.razorpaySignature()
                )
        );
    }
}