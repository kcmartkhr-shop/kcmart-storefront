package in.kcmart.backend.catalog.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "categories")
public class Category {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false, unique = true, length = 150)
    private String slug;

    @Column(name = "parent_id")
    private Long parentId;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "created_at", insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getSlug() { return slug; }
    public Long getParentId() { return parentId; }
    public boolean isActive() { return active; }
    public OffsetDateTime getCreatedAt() { return createdAt; }

    public void setId(Long id) { this.id = id; }
    public void setName(String name) { this.name = name; }
    public void setSlug(String slug) { this.slug = slug; }
    public void setParentId(Long parentId) { this.parentId = parentId; }
    public void setActive(boolean active) { this.active = active; }
}
