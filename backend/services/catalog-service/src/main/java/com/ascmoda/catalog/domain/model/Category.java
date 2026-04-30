package com.ascmoda.catalog.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.hibernate.annotations.BatchSize;

@Entity
@BatchSize(size = 50)
@Table(
        name = "categories",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_categories_slug", columnNames = "slug")
        },
        indexes = {
                @Index(name = "idx_categories_active", columnList = "is_active")
        }
)
public class Category extends BaseAuditableEntity {

    @Column(nullable = false, length = 160)
    private String name;

    @Column(nullable = false, length = 180)
    private String slug;

    @Column(length = 1000)
    private String description;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    protected Category() {
    }

    public Category(String name, String slug, String description, boolean active) {
        this.name = name;
        this.slug = slug;
        this.description = description;
        this.active = active;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
