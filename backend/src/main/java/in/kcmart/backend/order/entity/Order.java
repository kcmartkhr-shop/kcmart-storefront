package in.kcmart.backend.order.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "orders")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(
        name = "order_number",
        nullable = false,
        unique = true,
        length = 40
    )
    private String orderNumber;

    @Column(
        name = "idempotency_key",
        unique = true,
        length = 100
    )
    private String idempotencyKey;

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Column(
        name = "fulfillment_type",
        nullable = false,
        length = 30
    )
    private String fulfillmentType;

    @Column(
        name = "customer_name",
        nullable = false,
        length = 255
    )
    private String customerName;

    @Column(
        name = "customer_email",
        nullable = false,
        length = 320
    )
    private String customerEmail;

    @Column(
        name = "customer_phone",
        nullable = false,
        length = 20
    )
    private String customerPhone;

    @Column(name = "address_line1", length = 255)
    private String addressLine1;

    @Column(name = "address_line2", length = 255)
    private String addressLine2;

    @Column(length = 255)
    private String landmark;

    @Column(length = 120)
    private String city;

    @Column(length = 120)
    private String state;

    @Column(name = "postal_code", length = 20)
    private String postalCode;

    @Column(length = 80)
    private String country;

    @Column(
        nullable = false,
        precision = 12,
        scale = 2
    )
    private BigDecimal subtotal;

    @Column(
        name = "delivery_charge",
        nullable = false,
        precision = 12,
        scale = 2
    )
    private BigDecimal deliveryCharge;

    @Column(
        name = "total_amount",
        nullable = false,
        precision = 12,
        scale = 2
    )
    private BigDecimal totalAmount;

    @Column(
        name = "payment_method",
        nullable = false,
        length = 30
    )
    private String paymentMethod;

    @Column(
        name = "payment_status",
        nullable = false,
        length = 30
    )
    private String paymentStatus;

    @Column(
        name = "order_status",
        nullable = false,
        length = 30
    )
    private String orderStatus;

    @Column(
        name = "created_at",
        insertable = false,
        updatable = false
    )
    private OffsetDateTime createdAt;

    @Column(
        name = "updated_at",
        insertable = false,
        updatable = false
    )
    private OffsetDateTime updatedAt;

    public Long getId() {
        return id;
    }

    public String getOrderNumber() {
        return orderNumber;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public String getFulfillmentType() {
        return fulfillmentType;
    }

    public String getCustomerName() {
        return customerName;
    }

    public String getCustomerEmail() {
        return customerEmail;
    }

    public String getCustomerPhone() {
        return customerPhone;
    }

    public String getAddressLine1() {
        return addressLine1;
    }

    public String getAddressLine2() {
        return addressLine2;
    }

    public String getLandmark() {
        return landmark;
    }

    public String getCity() {
        return city;
    }

    public String getState() {
        return state;
    }

    public String getPostalCode() {
        return postalCode;
    }

    public String getCountry() {
        return country;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public BigDecimal getDeliveryCharge() {
        return deliveryCharge;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public String getPaymentStatus() {
        return paymentStatus;
    }

    public String getOrderStatus() {
        return orderStatus;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setOrderNumber(String orderNumber) {
        this.orderNumber = orderNumber;
    }

    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }

    public void setCustomerId(Long customerId) {
        this.customerId = customerId;
    }

    public void setFulfillmentType(String fulfillmentType) {
        this.fulfillmentType = fulfillmentType;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public void setCustomerEmail(String customerEmail) {
        this.customerEmail = customerEmail;
    }

    public void setCustomerPhone(String customerPhone) {
        this.customerPhone = customerPhone;
    }

    public void setAddressLine1(String addressLine1) {
        this.addressLine1 = addressLine1;
    }

    public void setAddressLine2(String addressLine2) {
        this.addressLine2 = addressLine2;
    }

    public void setLandmark(String landmark) {
        this.landmark = landmark;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public void setState(String state) {
        this.state = state;
    }

    public void setPostalCode(String postalCode) {
        this.postalCode = postalCode;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public void setSubtotal(BigDecimal subtotal) {
        this.subtotal = subtotal;
    }

    public void setDeliveryCharge(BigDecimal deliveryCharge) {
        this.deliveryCharge = deliveryCharge;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public void setPaymentStatus(String paymentStatus) {
        this.paymentStatus = paymentStatus;
    }

    public void setOrderStatus(String orderStatus) {
        this.orderStatus = orderStatus;
    }
}