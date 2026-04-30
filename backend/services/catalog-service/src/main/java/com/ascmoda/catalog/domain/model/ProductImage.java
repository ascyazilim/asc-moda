package com.ascmoda.catalog.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.BatchSize;

@Entity
@BatchSize(size = 50)
@Table(
        name = "product_images",
        indexes = {
                @Index(name = "idx_product_images_product_id", columnList = "product_id"),
                @Index(name = "idx_product_images_variant_id", columnList = "variant_id"),
                @Index(name = "idx_product_images_product_sort_order", columnList = "product_id, sort_order")
        }
)
public class ProductImage extends BaseAuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "variant_id")
    private ProductVariant variant;

    @Column(name = "image_url", nullable = false, length = 1000)
    private String imageUrl;

    @Column(name = "alt_text", length = 300)
    private String altText;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder = 0;

    @Column(name = "is_main", nullable = false)
    private boolean main;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    protected ProductImage() {
    }

    public ProductImage(ProductVariant variant, String imageUrl, String altText, int sortOrder,
                        boolean main, boolean active) {
        this.variant = variant;
        this.imageUrl = imageUrl;
        this.altText = altText;
        this.sortOrder = sortOrder;
        this.main = main;
        this.active = active;
    }

    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        this.product = product;
    }

    public ProductVariant getVariant() {
        return variant;
    }

    public void setVariant(ProductVariant variant) {
        this.variant = variant;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getAltText() {
        return altText;
    }

    public void setAltText(String altText) {
        this.altText = altText;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(int sortOrder) {
        this.sortOrder = sortOrder;
    }

    public boolean isMain() {
        return main;
    }

    public void setMain(boolean main) {
        this.main = main;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
