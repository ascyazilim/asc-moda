package com.ascmoda.inventory.domain.model;

import com.ascmoda.inventory.api.error.InvalidStockStateException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.hibernate.annotations.BatchSize;

import java.util.UUID;

@Entity
@BatchSize(size = 50)
@Table(
        name = "inventory_items",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_inventory_items_sku", columnNames = "sku"),
                @UniqueConstraint(name = "uk_inventory_items_product_variant_id", columnNames = "product_variant_id")
        },
        indexes = {
                @Index(name = "idx_inventory_items_product_variant_id", columnList = "product_variant_id"),
                @Index(name = "idx_inventory_items_active", columnList = "is_active")
        }
)
public class InventoryItem extends BaseAuditableEntity {

    @Column(name = "product_variant_id", nullable = false)
    private UUID productVariantId;

    @Column(nullable = false, length = 120)
    private String sku;

    @Column(name = "quantity_on_hand", nullable = false)
    private int quantityOnHand;

    @Column(name = "reserved_quantity", nullable = false)
    private int reservedQuantity;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    protected InventoryItem() {
    }

    public InventoryItem(UUID productVariantId, String sku, int quantityOnHand, int reservedQuantity, boolean active) {
        this.productVariantId = productVariantId;
        this.sku = sku;
        this.quantityOnHand = quantityOnHand;
        this.reservedQuantity = reservedQuantity;
        this.active = active;
        validateState();
    }

    public void update(UUID productVariantId, String sku, int quantityOnHand, int reservedQuantity, boolean active) {
        this.productVariantId = productVariantId;
        this.sku = sku;
        this.quantityOnHand = quantityOnHand;
        this.reservedQuantity = reservedQuantity;
        this.active = active;
        validateState();
    }

    public void increase(int quantity) {
        requirePositive(quantity);
        quantityOnHand += quantity;
        validateState();
    }

    public void decrease(int quantity) {
        requirePositive(quantity);
        if (quantity > availableQuantity()) {
            throw new InvalidStockStateException("Insufficient available stock for decrease");
        }
        quantityOnHand -= quantity;
        validateState();
    }

    public void reserve(int quantity) {
        requirePositive(quantity);
        if (quantity > availableQuantity()) {
            throw new InvalidStockStateException("Insufficient available stock for reservation");
        }
        reservedQuantity += quantity;
        validateState();
    }

    public void release(int quantity) {
        requirePositive(quantity);
        if (quantity > reservedQuantity) {
            throw new InvalidStockStateException("Release quantity cannot exceed reserved quantity");
        }
        reservedQuantity -= quantity;
        validateState();
    }

    public int availableQuantity() {
        return quantityOnHand - reservedQuantity;
    }

    private void validateState() {
        if (quantityOnHand < 0) {
            throw new InvalidStockStateException("Quantity on hand cannot be negative");
        }
        if (reservedQuantity < 0) {
            throw new InvalidStockStateException("Reserved quantity cannot be negative");
        }
        if (reservedQuantity > quantityOnHand) {
            throw new InvalidStockStateException("Reserved quantity cannot exceed quantity on hand");
        }
    }

    private void requirePositive(int quantity) {
        if (quantity <= 0) {
            throw new InvalidStockStateException("Stock quantity must be greater than zero");
        }
    }

    public UUID getProductVariantId() {
        return productVariantId;
    }

    public String getSku() {
        return sku;
    }

    public int getQuantityOnHand() {
        return quantityOnHand;
    }

    public int getReservedQuantity() {
        return reservedQuantity;
    }

    public boolean isActive() {
        return active;
    }
}
