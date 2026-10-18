package in.kcmart.backend.customer.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.OffsetDateTime;

@Entity
@Table(
    name = "customers",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_customers_provider_subject",
            columnNames = {"provider", "provider_subject"}
        ),
        @UniqueConstraint(
            name = "uk_customers_email",
            columnNames = {"email"}
        )
    }
)
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 30)
    private String provider;

    @Column(
        name = "provider_subject",
        nullable = false,
        length = 255
    )
    private String providerSubject;

    @Column(nullable = false, length = 320)
    private String email;

    @Column(length = 255)
    private String name;

    @Column(name = "image_url", columnDefinition = "TEXT")
    private String imageUrl;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected Customer() {
    }

    public Customer(
        String provider,
        String providerSubject,
        String email,
        String name,
        String imageUrl
    ) {
        this.provider = provider;
        this.providerSubject = providerSubject;
        this.email = email;
        this.name = name;
        this.imageUrl = imageUrl;
        this.active = true;
    }

    @PrePersist
    void onCreate() {
        OffsetDateTime now = OffsetDateTime.now();

        if (createdAt == null) {
            createdAt = now;
        }

        if (updatedAt == null) {
            updatedAt = now;
        }
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public String getProvider() {
        return provider;
    }

    public String getProviderSubject() {
        return providerSubject;
    }

    public String getEmail() {
        return email;
    }

    public String getName() {
        return name;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public boolean isActive() {
        return active;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void updateProfile(
        String email,
        String name,
        String imageUrl
    ) {
        this.email = email;
        this.name = name;
        this.imageUrl = imageUrl;
    }

    public void activate() {
        this.active = true;
    }

    public void deactivate() {
        this.active = false;
    }
}