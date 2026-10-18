package in.kcmart.backend.order.controller;

import in.kcmart.backend.customer.entity.Customer;
import in.kcmart.backend.customer.repository.CustomerRepository;
import in.kcmart.backend.order.dto.CheckoutValidationResponse;
import in.kcmart.backend.order.dto.CheckoutDetailsValidationResponse;
import in.kcmart.backend.order.dto.InternalCheckoutValidationRequest;
import in.kcmart.backend.order.dto.InternalCheckoutDetailsRequest;
import in.kcmart.backend.order.dto.InternalCheckoutPaymentMethodRequest;
import in.kcmart.backend.order.dto.CheckoutPaymentMethodValidationResponse;
import in.kcmart.backend.order.dto.InternalOrderCreateRequest;
import in.kcmart.backend.order.dto.OrderCreateResponse;
import in.kcmart.backend.order.service.CheckoutValidationService;
import in.kcmart.backend.order.service.OrderCreationService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/internal/checkout")
public class InternalCheckoutController {

    private final CustomerRepository customers;
    private final CheckoutValidationService checkoutValidation;
    private final OrderCreationService orderCreation;

    public InternalCheckoutController(
        CustomerRepository customers,
        CheckoutValidationService checkoutValidation,
        OrderCreationService orderCreation
    ) {
        this.customers = customers;
        this.checkoutValidation = checkoutValidation;
        this.orderCreation = orderCreation;
    }

    @PostMapping("/validate")
    public ResponseEntity<CheckoutValidationResponse> validate(
        @Valid
        @RequestBody
        InternalCheckoutValidationRequest request
    ) {

        Customer customer =
            customers.findByEmailIgnoreCase(
                request.customerEmail()
            )
            .orElseThrow(() ->
                new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Customer not found"
                )
            );

        if (!customer.isActive()) {
            throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "Customer account is inactive"
            );
        }

        CheckoutValidationResponse response =
            checkoutValidation.validate(
                request.toCheckoutRequest(),
                request.fulfillmentType().trim()
            );

        return ResponseEntity.ok(response);
    }

    @PostMapping("/details")
    public ResponseEntity<CheckoutDetailsValidationResponse> validateDetails(
        @Valid
        @RequestBody
        InternalCheckoutDetailsRequest request
    ) {

        Customer customer =
            customers.findByEmailIgnoreCase(
                request.customerEmail().trim()
            )
            .orElseThrow(() ->
                new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Customer not found"
                )
            );

        if (!customer.isActive()) {
            throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "Customer account is inactive"
            );
        }

        String fulfillment =
            request.fulfillmentType().trim();

        String name =
            requireText(
                request.customerName(),
                "Customer name",
                2
            );

        String phone =
            request.customerPhone().trim();

        if (!phone.matches("^[6-9][0-9]{9}$")) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Invalid mobile number"
            );
        }

        String addressLine1 = null;
        String addressLine2 = null;
        String landmark = null;
        String city = null;
        String state = null;
        String postalCode = null;
        String country = null;

        if ("DELIVERY".equals(fulfillment)) {

            addressLine1 =
                requireText(
                    request.addressLine1(),
                    "Address line 1",
                    5
                );

            addressLine2 =
                normalizeOptional(
                    request.addressLine2()
                );

            landmark =
                normalizeOptional(
                    request.landmark()
                );

            city =
                requireText(
                    request.city(),
                    "City",
                    2
                );

            state =
                requireText(
                    request.state(),
                    "State",
                    2
                );

            postalCode =
                request.postalCode() == null
                    ? ""
                    : request.postalCode().trim();

            if (!postalCode.matches("^[0-9]{6}$")) {
                throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid PIN code"
                );
            }

            String suppliedCountry =
                request.country() == null
                    ? ""
                    : request.country().trim();

            if (!"India".equalsIgnoreCase(
                suppliedCountry
            )) {
                throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Delivery country must be India"
                );
            }

            country = "India";

        } else if ("STORE_PICKUP".equals(fulfillment)) {

            /*
             * Store pickup intentionally does not trust or
             * persist a customer-supplied delivery address.
             *
             * The store location is KCMART business
             * configuration, not browser address data.
             */

        } else {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Invalid fulfillment type"
            );
        }

        return ResponseEntity.ok(
            new CheckoutDetailsValidationResponse(
                true,
                fulfillment,
                name,
                phone,
                addressLine1,
                addressLine2,
                landmark,
                city,
                state,
                postalCode,
                country
            )
        );
    }

    @PostMapping("/payment-method")
    public ResponseEntity<CheckoutPaymentMethodValidationResponse> validatePaymentMethod(
        @Valid
        @RequestBody
        InternalCheckoutPaymentMethodRequest request
    ) {

        Customer customer =
            customers.findByEmailIgnoreCase(
                request.customerEmail().trim()
            )
            .orElseThrow(() ->
                new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Customer not found"
                )
            );

        if (!customer.isActive()) {
            throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "Customer account is inactive"
            );
        }

        String fulfillmentType =
            request.fulfillmentType().trim();

        String paymentMethod =
            request.paymentMethod().trim();

        if ("DELIVERY".equals(fulfillmentType)) {

            if (!"ONLINE".equals(paymentMethod)) {
                throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Home Delivery supports Online Payment only"
                );
            }

        } else if ("STORE_PICKUP".equals(fulfillmentType)) {

            if (
                !"ONLINE".equals(paymentMethod)
                &&
                !"PAY_AT_STORE".equals(paymentMethod)
            ) {
                throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid payment method for Store Pickup"
                );
            }

        } else {

            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Invalid fulfillment type"
            );
        }

        return ResponseEntity.ok(
            new CheckoutPaymentMethodValidationResponse(
                true,
                fulfillmentType,
                paymentMethod
            )
        );
    }

    @PostMapping("/order")
    public ResponseEntity<OrderCreateResponse> createOrder(
        @Valid
        @RequestBody
        InternalOrderCreateRequest request
    ) {

        OrderCreateResponse response =
            orderCreation.createStorePickupOrder(
                request
            );

        return ResponseEntity.ok(response);
    }

    private String requireText(
        String value,
        String fieldName,
        int minimumLength
    ) {

        if (value == null) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                fieldName + " is required"
            );
        }

        String normalized = value.trim();

        if (normalized.length() < minimumLength) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                fieldName + " is invalid"
            );
        }

        return normalized;
    }

    private String normalizeOptional(
        String value
    ) {

        if (value == null) {
            return null;
        }

        String normalized = value.trim();

        return normalized.isEmpty()
            ? null
            : normalized;
    }
}