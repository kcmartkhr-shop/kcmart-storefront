package in.kcmart.backend.order.entity;

import jakarta.persistence.*;

import java.time.OffsetDateTime;

@Entity
@Table(name = "order_notifications")
public class OrderNotification {
    @jakarta.persistence.Column(name = "request_key", length = 100)
    private String requestKey;

    public String getRequestKey() {
        return requestKey;
    }

    public void setRequestKey(String requestKey) {
        this.requestKey = requestKey;
    }


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Column(name = "notification_type", nullable = false, length = 50)
    private String notificationType;

    @Column(name = "channel", nullable = false, length = 20)
    private String channel;

    @Column(name = "recipient", nullable = false, length = 320)
    private String recipient;

    @Column(name = "subject", nullable = false, length = 255)
    private String subject;

    @Column(name = "status", nullable = false, length = 20)
    private String status = "PENDING";

    @Column(name = "attempt_count", nullable = false)
    private Integer attemptCount = 0;

    @Column(name = "last_error", length = 1000)
    private String lastError;

    @Column(
        name = "created_at",
        nullable = false,
        insertable = false,
        updatable = false
    )
    private OffsetDateTime createdAt;

    @Column(name = "sent_at")
    private OffsetDateTime sentAt;

    public Long getId() {
        return id;
    }

    public Long getOrderId() {
        return orderId;
    }

    public String getNotificationType() {
        return notificationType;
    }

    public String getChannel() {
        return channel;
    }

    public String getRecipient() {
        return recipient;
    }

    public String getSubject() {
        return subject;
    }

    public String getStatus() {
        return status;
    }

    public Integer getAttemptCount() {
        return attemptCount;
    }

    public String getLastError() {
        return lastError;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getSentAt() {
        return sentAt;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public void setNotificationType(String notificationType) {
        this.notificationType = notificationType;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public void setRecipient(String recipient) {
        this.recipient = recipient;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setAttemptCount(Integer attemptCount) {
        this.attemptCount = attemptCount;
    }

    public void setLastError(String lastError) {
        this.lastError = lastError;
    }

    public void setSentAt(OffsetDateTime sentAt) {
        this.sentAt = sentAt;
    }
}