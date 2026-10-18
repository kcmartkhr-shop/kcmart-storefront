package in.kcmart.backend.payment.razorpay.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "razorpay_refund_audit_events")
public class RazorpayRefundAuditEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "refund_attempt_id", nullable = false)
    private Long refundAttemptId;

    @Column(name = "event_type", nullable = false)
    private String eventType;

    @Column(name = "previous_status")
    private String previousStatus;

    @Column(name = "new_status")
    private String newStatus;

    @Column(name = "actor_type", nullable = false)
    private String actorType;

    @Column(name = "actor_identifier")
    private String actorIdentifier;

    @Column(name = "event_note")
    private String eventNote;

    @Column(name = "created_at", insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    public Long getId() { return id; }

    public Long getRefundAttemptId() { return refundAttemptId; }
    public void setRefundAttemptId(Long value) { refundAttemptId = value; }

    public String getEventType() { return eventType; }
    public void setEventType(String value) { eventType = value; }

    public String getPreviousStatus() { return previousStatus; }
    public void setPreviousStatus(String value) { previousStatus = value; }

    public String getNewStatus() { return newStatus; }
    public void setNewStatus(String value) { newStatus = value; }

    public String getActorType() { return actorType; }
    public void setActorType(String value) { actorType = value; }

    public String getActorIdentifier() { return actorIdentifier; }
    public void setActorIdentifier(String value) { actorIdentifier = value; }

    public String getEventNote() { return eventNote; }
    public void setEventNote(String value) { eventNote = value; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
}