package in.kcmart.backend.payment.razorpay.service;

import in.kcmart.backend.customer.entity.Customer;
import in.kcmart.backend.customer.repository.CustomerRepository;
import in.kcmart.backend.order.entity.Order;
import in.kcmart.backend.order.repository.OrderRepository;
import in.kcmart.backend.payment.razorpay.client.RazorpayClient;
import in.kcmart.backend.payment.razorpay.client.RazorpayClient.RazorpayOrderResponse;
import in.kcmart.backend.payment.razorpay.client.RazorpayClient.RazorpayPaymentResponse;
import in.kcmart.backend.payment.razorpay.config.RazorpayProperties;
import in.kcmart.backend.payment.razorpay.entity.RazorpayPaymentAttempt;
import in.kcmart.backend.payment.razorpay.repository.RazorpayPaymentAttemptRepository;
import in.kcmart.backend.payment.razorpay.security.RazorpaySignatureVerifier;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class RazorpayPaymentAttemptService {

    private final CustomerRepository customers;
    private final OrderRepository orders;
    private final RazorpayPaymentAttemptRepository attempts;
    private final RazorpayClient razorpayClient;
    private final RazorpayProperties properties;
    private final RazorpaySignatureVerifier signatureVerifier;
    private final RazorpayPaymentFinalizationService paymentFinalizer;
    private final RazorpayPaymentReconciliationService reconciliationService;

    public RazorpayPaymentAttemptService(
            CustomerRepository customers,
            OrderRepository orders,
            RazorpayPaymentAttemptRepository attempts,
            RazorpayClient razorpayClient,
            RazorpayProperties properties,
            RazorpaySignatureVerifier signatureVerifier,
            RazorpayPaymentFinalizationService paymentFinalizer,
            RazorpayPaymentReconciliationService reconciliationService) {

        this.customers = customers;
        this.orders = orders;
        this.attempts = attempts;
        this.razorpayClient = razorpayClient;
        this.properties = properties;
        this.signatureVerifier = signatureVerifier;
        this.paymentFinalizer = paymentFinalizer;
        this.reconciliationService = reconciliationService;
    }

    public CheckoutOrder createCheckoutOrder(
            String trustedEmail,
            Long orderId) {

        PreparedOrder prepared =
                prepareOrder(trustedEmail, orderId);

        RazorpayPaymentAttempt existing =
                attempts
                    .findFirstByOrderIdAndStatusOrderByIdDesc(
                        prepared.orderId(),
                        "CREATED"
                    )
                    .orElse(null);

        if (existing != null) {
            validateReusableAttempt(
                    existing,
                    prepared
            );

            return toCheckoutOrder(
                    existing,
                    prepared.orderNumber()
            );
        }

        Map<String, String> notes =
                new LinkedHashMap<>();

        notes.put(
                "kcmart_order_id",
                prepared.orderId().toString()
        );

        notes.put(
                "kcmart_order_number",
                prepared.orderNumber()
        );

        RazorpayOrderResponse gatewayOrder;

        try {
            gatewayOrder =
                    razorpayClient.createOrder(
                        prepared.amountPaise(),
                        prepared.receipt(),
                        notes
                    );
        } catch (Exception ex) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Unable to create payment order.",
                    ex
            );
        }

        validateGatewayOrder(
                gatewayOrder,
                prepared
        );

        RazorpayPaymentAttempt attempt =
                new RazorpayPaymentAttempt();

        attempt.setOrderId(
                prepared.orderId()
        );

        attempt.setRazorpayOrderId(
                gatewayOrder.id()
        );

        attempt.setAmountPaise(
                prepared.amountPaise()
        );

        attempt.setCurrency("INR");
        attempt.setStatus("CREATED");
        attempt.setSignatureVerified(false);

        try {
            RazorpayPaymentAttempt saved =
                    attempts.saveAndFlush(attempt);

            return toCheckoutOrder(
                    saved,
                    prepared.orderNumber()
            );

        } catch (DataIntegrityViolationException ex) {

            RazorpayPaymentAttempt winner =
                    attempts
                        .findFirstByOrderIdAndStatusOrderByIdDesc(
                            prepared.orderId(),
                            "CREATED"
                        )
                        .orElseThrow(() ->
                            new ResponseStatusException(
                                HttpStatus.CONFLICT,
                                "A payment attempt is already being created. Please try again.",
                                ex
                            )
                        );

            validateReusableAttempt(
                    winner,
                    prepared
            );

            return toCheckoutOrder(
                    winner,
                    prepared.orderNumber()
            );
        }
    }

    public VerificationResult verifyCheckoutSignature(
            String trustedEmail,
            Long orderId,
            String razorpayOrderId,
            String razorpayPaymentId,
            String razorpaySignature) {

        if (orderId == null || orderId <= 0) {
            throw orderNotFound();
        }

        Customer customer =
                resolveActiveCustomer(trustedEmail);

        Order order =
                orders.findById(orderId)
                        .orElseThrow(this::orderNotFound);

        if (!customer.getId().equals(
                order.getCustomerId())) {

            throw orderNotFound();
        }

        if (!"ONLINE".equals(
                order.getPaymentMethod())) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "This order does not use online payment."
            );
        }

        /*
         * PAID is deliberately allowed here.
         *
         * A repeated legitimate Razorpay callback can arrive
         * after the first callback or webhook has already
         * finalized the order.
         */
        boolean cancelledUnpaidDelivery =
                "DELIVERY".equals(order.getFulfillmentType())
                && "CANCELLED".equals(order.getOrderStatus())
                && "FAILED".equals(order.getPaymentStatus());

        if (!"PENDING".equals(order.getPaymentStatus())
                && !"PAID".equals(order.getPaymentStatus())
                && !cancelledUnpaidDelivery) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Payment cannot be verified from the current state."
            );
        }

        if (("CANCELLED".equals(order.getOrderStatus())
                && !cancelledUnpaidDelivery)
                || "COMPLETED".equals(order.getOrderStatus())) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Payment cannot be verified for this order."
            );
        }

        if (razorpayOrderId == null
                || razorpayOrderId.isBlank()
                || razorpayPaymentId == null
                || razorpayPaymentId.isBlank()
                || razorpaySignature == null
                || razorpaySignature.isBlank()) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Razorpay payment verification data is required."
            );
        }

        String suppliedOrderId =
                razorpayOrderId.trim();

        String suppliedPaymentId =
                razorpayPaymentId.trim();

        /*
         * This is intentionally a NON-LOCKING read.
         *
         * No database transaction or pessimistic lock is held
         * while KCMART talks to Razorpay over the network.
         */
        RazorpayPaymentAttempt attempt =
                attempts.findByRazorpayOrderId(
                        suppliedOrderId
                ).orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.BAD_REQUEST,
                                "Payment verification failed."
                        )
                );

        if (!orderId.equals(
                attempt.getOrderId())) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Payment verification failed."
            );
        }

        /*
         * HMAC uses the Razorpay Order ID stored in our DB.
         * The browser value is only used to locate the
         * candidate attempt and must match that stored row.
         */
        boolean verified =
                signatureVerifier.verifyPaymentSignature(
                        attempt.getRazorpayOrderId(),
                        suppliedPaymentId,
                        razorpaySignature.trim()
                );

        if (!verified) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Payment signature verification failed."
            );
        }

        /*
         * External network call.
         *
         * IMPORTANT: there is NO surrounding transaction and
         * NO pessimistic DB lock while this request is made.
         */
        RazorpayPaymentResponse gatewayPayment;

        try {
            gatewayPayment =
                    razorpayClient.fetchPayment(
                            suppliedPaymentId
                    );

        } catch (Exception ex) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Unable to confirm payment with payment provider.",
                    ex
            );
        }

        validateGatewayPayment(
                gatewayPayment,
                attempt,
                suppliedPaymentId
        );

        /*
         * Only now enter the short atomic DB transaction.
         *
         * The finalizer re-locks and revalidates:
         *
         * ORDER first
         * ATTEMPT second
         *
         * This protects against state changes that may have
         * happened while the Razorpay network request was
         * running.
         */
        try {
            RazorpayPaymentFinalizationService.FinalizationResult result =
                    paymentFinalizer.finalizeCapturedPayment(
                            order.getId(),
                            attempt.getRazorpayOrderId(),
                            gatewayPayment.id(),
                            gatewayPayment.amount(),
                            gatewayPayment.currency(),
                            true
                    );

            return new VerificationResult(
                    result.orderId(),
                    result.razorpayOrderId(),
                    result.razorpayPaymentId(),
                    true
            );

        } catch (ResponseStatusException ex) {

            if (ex.getStatusCode().value() != 409) {
                throw ex;
            }

            /*
             * This payment was already signature-verified
             * and independently fetched from Razorpay.
             *
             * Reconciliation locks the order and attempt,
             * then requires an expired or cancelled order.
             *
             * Unexpected conflicts are never converted
             * into successful payment responses.
             */
            reconciliationService.recordCapturedForCancelledOrder(
                    order.getId(),
                    attempt.getRazorpayOrderId(),
                    gatewayPayment
            );

            /*
             * Until the customer UI supports a separate
             * reconciliation outcome, return HTTP 409.
             *
             * This prevents the existing frontend from
             * clearing the cart or confirming the order.
             */
            ResponseStatusException reviewException =
                    new ResponseStatusException(
                            HttpStatus.CONFLICT,
                            "Payment captured, but the order expired. The payment is under review."
                    );

            reviewException.getHeaders().add(
                    "X-KCMART-Payment-Outcome",
                    "RECONCILIATION_REQUIRED"
            );

            throw reviewException;
        }
    }
    private PreparedOrder prepareOrder(
            String trustedEmail,
            Long orderId) {

        if (orderId == null || orderId <= 0) {
            throw orderNotFound();
        }

        Customer customer =
                resolveActiveCustomer(
                    trustedEmail
                );

        Order order =
                orders.findById(orderId)
                    .orElseThrow(
                        this::orderNotFound
                    );

        if (!customer.getId().equals(
                order.getCustomerId())) {

            throw orderNotFound();
        }

        if (!"ONLINE".equals(
                order.getPaymentMethod())) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "This order does not use online payment."
            );
        }

        if (!"PENDING".equals(
                order.getPaymentStatus())) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Payment is not pending."
            );
        }

        if ("CANCELLED".equals(
                order.getOrderStatus())
                || "COMPLETED".equals(
                    order.getOrderStatus())) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Payment cannot be started for this order."
            );
        }

        long amountPaise =
                toPaise(
                    order.getTotalAmount()
                );

        return new PreparedOrder(
                order.getId(),
                order.getOrderNumber(),
                amountPaise,
                buildReceipt(order)
        );
    }

    private void validateReusableAttempt(
            RazorpayPaymentAttempt attempt,
            PreparedOrder prepared) {

        if (!Long.valueOf(
                prepared.amountPaise()
            ).equals(
                attempt.getAmountPaise())) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Existing payment attempt amount does not match this order."
            );
        }

        if (!"INR".equals(
                attempt.getCurrency())) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Existing payment attempt currency is invalid."
            );
        }

        if (!"CREATED".equals(
                attempt.getStatus())) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Existing payment attempt is not reusable."
            );
        }
    }

    private void validateGatewayPayment(
            RazorpayPaymentResponse payment,
            RazorpayPaymentAttempt attempt,
            String expectedPaymentId) {

        if (payment == null
                || payment.id() == null
                || !payment.id().equals(expectedPaymentId)) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Payment provider returned an unexpected payment."
            );
        }

        if (payment.order_id() == null
                || !payment.order_id().equals(
                    attempt.getRazorpayOrderId())) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Payment does not belong to this order."
            );
        }

        if (payment.amount() == null
                || !payment.amount().equals(
                    attempt.getAmountPaise())) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Payment amount does not match this order."
            );
        }

        if (!"INR".equals(payment.currency())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Payment currency does not match this order."
            );
        }

        if (!"captured".equalsIgnoreCase(
                payment.status())) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Payment has not been captured."
            );
        }

        if (!Boolean.TRUE.equals(
                payment.captured())) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Payment is not captured."
            );
        }
    }
    private void validateGatewayOrder(
            RazorpayOrderResponse gatewayOrder,
            PreparedOrder prepared) {

        if (gatewayOrder == null
                || gatewayOrder.id() == null
                || gatewayOrder.id().isBlank()) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Payment provider returned an invalid order."
            );
        }

        if (gatewayOrder.amount() == null
                || gatewayOrder.amount()
                    .longValue()
                    != prepared.amountPaise()) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Payment provider returned an unexpected amount."
            );
        }

        if (!"INR".equals(
                gatewayOrder.currency())) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Payment provider returned an unexpected currency."
            );
        }
    }

    private CheckoutOrder toCheckoutOrder(
            RazorpayPaymentAttempt attempt,
            String orderNumber) {

        return new CheckoutOrder(
                attempt.getOrderId(),
                orderNumber,
                attempt.getRazorpayOrderId(),
                attempt.getAmountPaise(),
                attempt.getCurrency(),
                properties.keyId()
        );
    }

    private Customer resolveActiveCustomer(
            String trustedEmail) {

        String email =
                normalizeEmail(
                    trustedEmail
                );

        Customer customer =
                customers
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

    private long toPaise(
            BigDecimal amount) {

        if (amount == null
                || amount.signum() <= 0) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Order amount is invalid."
            );
        }

        try {
            return amount
                    .setScale(
                        2,
                        RoundingMode.UNNECESSARY
                    )
                    .movePointRight(2)
                    .longValueExact();

        } catch (ArithmeticException ex) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Order amount cannot be converted to paise exactly.",
                    ex
            );
        }
    }

    private String buildReceipt(
            Order order) {

        String receipt =
                "KCM-" + order.getId();

        if (receipt.length() > 40) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Order receipt identifier is invalid."
            );
        }

        return receipt;
    }

    private String normalizeEmail(
            String value) {

        if (value == null
                || value.isBlank()) {

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

    private ResponseStatusException
            orderNotFound() {

        return new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Order not found."
        );
    }

    private record PreparedOrder(
            Long orderId,
            String orderNumber,
            long amountPaise,
            String receipt) {
    }

    public record VerificationResult(
            Long orderId,
            String razorpayOrderId,
            String razorpayPaymentId,
            boolean signatureVerified) {
    }
    public record CheckoutOrder(
            Long orderId,
            String orderNumber,
            String razorpayOrderId,
            long amountPaise,
            String currency,
            String keyId) {
    }
}