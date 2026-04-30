package com.ascmoda.inventory.domain.model;

import com.ascmoda.inventory.api.error.InvalidStockStateException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.hibernate.annotations.BatchSize;

import java.time.Instant;
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

    @Column(name = "low_stock_threshold", nullable = false)
    private int lowStockThreshold = 5;

    @Column(name = "last_stock_change_at")
    private Instant lastStockChangeAt;

    protected InventoryItem() {
    }

    public InventoryItem(UUID productVariantId, String sku, int quantityOnHand, int reservedQuantity, boolean active) {
        this(productVariantId, sku, quantityOnHand, reservedQuantity, active, 5);
    }

    public InventoryItem(UUID productVariantId, String sku, int quantityOnHand, int reservedQuantity, boolean active,
                         int lowStockThreshold) {
        this.productVariantId = productVariantId;
        this.sku = sku;
        this.quantityOnHand = quantityOnHand;
        this.reservedQuantity = reservedQuantity;
        this.active = active;
        this.lowStockThreshold = normalizeLowStockThreshold(lowStockThreshold);
        this.lastStockChangeAt = Instant.now();
        validateState();
    }

    public void update(UUID productVariantId, String sku, int quantityOnHand, int reservedQuantity, boolean active) {
        update(productVariantId, sku, quantityOnHand, reservedQuantity, active, lowStockThreshold);
    }

    public void update(UUID productVariantId, String sku, int quantityOnHand, int reservedQuantity, boolean active,
                       int lowStockThreshold) {
        int previousQuantityOnHand = this.quantityOnHand;
        int previousReservedQuantity = this.reservedQuantity;
        this.productVariantId = productVariantId;
        this.sku = sku;
        this.quantityOnHand = quantityOnHand;
        this.reservedQuantity = reservedQuantity;
        this.active = active;
        this.lowStockThreshold = normalizeLowStockThreshold(lowStockThreshold);
        validateState();
        touchStockChangeIfChanged(previousQuantityOnHand, previousReservedQuantity);
    }

    public void increase(int quantity) {
        requirePositive(quantity);
        ensureActive();
        quantityOnHand += quantity;
        touchStockChange();
        validateState();
    }

    public void decrease(int quantity) {
        requirePositive(quantity);
        ensureActive();
        if (quantity > availableQuantity()) {
            throw new InvalidStockStateException("Insufficient available stock for decrease");
        }
        quantityOnHand -= quantity;
        touchStockChange();
        validateState();
    }

    public void adjustQuantityOnHand(int quantityOnHand) {
        if (quantityOnHand < reservedQuantity) {
            throw new InvalidStockStateException("Adjusted quantity cannot be lower than reserved quantity");
        }
        if (quantityOnHand < 0) {
            throw new InvalidStockStateException("Quantity on hand cannot be negative");
        }
        if (this.quantityOnHand == quantityOnHand) {
            throw new InvalidStockStateException("Adjustment must change quantity on hand");
        }
        this.quantityOnHand = quantityOnHand;
        touchStockChange();
        validateState();
    }

    public void reserve(int quantity) {
        requirePositive(quantity);
        ensureActive();
        if (quantity > availableQuantity()) {
            throw new InvalidStockStateException("Insufficient available stock for reservation");
        }
        reservedQuantity += quantity;
        touchStockChange();
        validateState();
    }

    public void release(int quantity) {
        requirePositive(quantity);
        if (quantity > reservedQuantity) {
            throw new InvalidStockStateException("Release quantity cannot exceed reserved quantity");
        }
        reservedQuantity -= quantity;
        touchStockChange();
        validateState();
    }

    public void consumeReserved(int quantity) {
        requirePositive(quantity);
        ensureActive();
        if (quantity > reservedQuantity) {
            throw new InvalidStockStateException("Consume quantity cannot exceed reserved quantity");
        }
        reservedQuantity -= quantity;
        quantityOnHand -= quantity;
        touchStockChange();
        validateState();
    }

    public int availableQuantity() {
        return quantityOnHand - reservedQuantity;
    }

    public boolean isLowStock() {
        return active && availableQuantity() <= lowStockThreshold;
    }

    public void activate() {
        active = true;
    }

    public void deactivate() {
        active = false;
    }

    public void ensureActive() {
        if (!active) {
            throw new InvalidStockStateException("Inventory item is inactive");
        }
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

    private int normalizeLowStockThreshold(int threshold) {
        if (threshold < 0) {
            throw new InvalidStockStateException("Low stock threshold cannot be negative");
        }
        return threshold;
    }

    private void touchStockChange() {
        lastStockChangeAt = Instant.now();
    }

    private void touchStockChangeIfChanged(int previousQuantityOnHand, int previousReservedQuantity) {
        if (previousQuantityOnHand != quantityOnHand || previousReservedQuantity != reservedQuantity) {
            touchStockChange();
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

    public int getLowStockThreshold() {
        return lowStockThreshold;
    }

    public Instant getLastStockChangeAt() {
        return lastStockChangeAt;
    }
}
