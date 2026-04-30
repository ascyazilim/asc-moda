package com.ascmoda.catalog.domain.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.hibernate.annotations.BatchSize;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@BatchSize(size = 50)
@Table(
        name = "products",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_products_slug", columnNames = "slug")
        },
        indexes = {
                @Index(name = "idx_products_category_id", columnList = "category_id"),
                @Index(name = "idx_products_status", columnList = "status")
        }
)
public class Product extends BaseAuditableEntity {

    @Column(nullable = false, length = 220)
    private String name;

    @Column(nullable = false, length = 240)
    private String slug;

    @Column(length = 5000)
    private String description;

    @Column(name = "short_description", length = 500)
    private String shortDescription;

    @Column(name = "base_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal basePrice;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ProductStatus status = ProductStatus.DRAFT;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL)
    @BatchSize(size = 50)
    private List<ProductVariant> variants = new ArrayList<>();

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL)
    @BatchSize(size = 50)
    private List<ProductImage> images = new ArrayList<>();

    protected Product() {
    }

    public Product(String name, String slug, String description, String shortDescription, BigDecimal basePrice,
                   ProductStatus status, Category category) {
        this.name = name;
        this.slug = slug;
        this.description = description;
        this.shortDescription = shortDescription;
        this.basePrice = basePrice;
        this.status = status;
        this.category = category;
    }

    public void addVariant(ProductVariant variant) {
        variant.setProduct(this);
        variants.add(variant);
    }

    public void deactivateVariants() {
        variants.forEach(variant -> variant.setActive(false));
    }

    public void addImage(ProductImage image) {
        image.setProduct(this);
        images.add(image);
    }

    public void deactivateImages() {
        images.forEach(image -> image.setActive(false));
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

    public String getShortDescription() {
        return shortDescription;
    }

    public void setShortDescription(String shortDescription) {
        this.shortDescription = shortDescription;
    }

    public BigDecimal getBasePrice() {
        return basePrice;
    }

    public void setBasePrice(BigDecimal basePrice) {
        this.basePrice = basePrice;
    }

    public ProductStatus getStatus() {
        return status;
    }

    public void setStatus(ProductStatus status) {
        this.status = status;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public List<ProductVariant> getVariants() {
        return variants;
    }

    public List<ProductImage> getImages() {
        return images;
    }
}
