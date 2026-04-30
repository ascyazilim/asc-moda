package com.ascmoda.catalog.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.hibernate.annotations.BatchSize;

import java.math.BigDecimal;

@Entity
@BatchSize(size = 50)
@Table(
        name = "product_variants",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_product_variants_sku", columnNames = "sku")
        },
        indexes = {
                @Index(name = "idx_product_variants_product_id", columnList = "product_id"),
                @Index(name = "idx_product_variants_active", columnList = "is_active")
        }
)
public class ProductVariant extends BaseAuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false, length = 120)
    private String sku;

    @Column(length = 80)
    private String color;

    @Column(length = 40)
    private String size;

    @Column(name = "stock_keeping_note", length = 500)
    private String stockKeepingNote;

    @Column(name = "price_override", precision = 12, scale = 2)
    private BigDecimal priceOverride;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    protected ProductVariant() {
    }

    public ProductVariant(String sku, String color, String size, String stockKeepingNote,
                          BigDecimal priceOverride, boolean active) {
        this.sku = sku;
        this.color = color;
        this.size = size;
        this.stockKeepingNote = stockKeepingNote;
        this.priceOverride = priceOverride;
        this.active = active;
    }

    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        this.product = product;
    }

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public String getSize() {
        return size;
    }

    public void setSize(String size) {
        this.size = size;
    }

    public String getStockKeepingNote() {
        return stockKeepingNote;
    }

    public void setStockKeepingNote(String stockKeepingNote) {
        this.stockKeepingNote = stockKeepingNote;
    }

    public BigDecimal getPriceOverride() {
        return priceOverride;
    }

    public void setPriceOverride(BigDecimal priceOverride) {
        this.priceOverride = priceOverride;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
