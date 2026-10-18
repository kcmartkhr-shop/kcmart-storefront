package in.kcmart.backend.order.entity;

import jakarta.persistence.*;

import java.time.OffsetDateTime;

@Entity
@Table(name = "order_payment_history")
public class OrderPaymentHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Column(name = "previous_status", length = 30)
    private String previousStatus;

    @Column(name = "new_status", nullable = false, length = 30)
    private String newStatus;

    @Column(name = "payment_method", nullable = false, length = 30)
    private String paymentMethod;

    @Column(name = "actor_type", nullable = false, length = 30)
    private String actorType;

    @Column(name = "actor_identifier", length = 320)
    private String actorIdentifier;

    @Column(name = "note", length = 1000)
    private String note;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    public Long getId() {
        return id;
    }

    public Long getOrderId() {
        return orderId;
    }

    public String getPreviousStatus() {
        return previousStatus;
    }

    public String getNewStatus() {
        return newStatus;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public String getActorType() {
        return actorType;
    }

    public String getActorIdentifier() {
        return actorIdentifier;
    }

    public String getNote() {
        return note;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public void setPreviousStatus(String previousStatus) {
        this.previousStatus = previousStatus;
    }

    public void setNewStatus(String newStatus) {
        this.newStatus = newStatus;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public void setActorType(String actorType) {
        this.actorType = actorType;
    }

    public void setActorIdentifier(String actorIdentifier) {
        this.actorIdentifier = actorIdentifier;
    }

    public void setNote(String note) {
        this.note = note;
    }
}