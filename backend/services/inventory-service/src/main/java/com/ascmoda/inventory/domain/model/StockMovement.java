package com.ascmoda.inventory.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.BatchSize;

import java.util.UUID;

@Entity
@BatchSize(size = 50)
@Table(
        name = "stock_movements",
        indexes = {
                @Index(name = "idx_stock_movements_inventory_item_id", columnList = "inventory_item_id"),
                @Index(name = "idx_stock_movements_sku", columnList = "sku"),
                @Index(name = "idx_stock_movements_created_at", columnList = "created_at")
        }
)
public class StockMovement extends BaseAuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "inventory_item_id", nullable = false)
    private InventoryItem inventoryItem;

    @Column(name = "product_variant_id", nullable = false)
    private UUID productVariantId;

    @Column(nullable = false, length = 120)
    private String sku;

    @Enumerated(EnumType.STRING)
    @Column(name = "movement_type", nullable = false, length = 30)
    private StockMovementType movementType;

    @Column(nullable = false)
    private int quantity;

    @Column(length = 1000)
    private String note;

    @Enumerated(EnumType.STRING)
    @Column(name = "reference_type", nullable = false, length = 30)
    private ReferenceType referenceType;

    @Column(name = "reference_id", length = 120)
    private String referenceId;

    protected StockMovement() {
    }

    public StockMovement(InventoryItem inventoryItem, StockMovementType movementType, int quantity, String note,
                         ReferenceType referenceType, String referenceId) {
        this.inventoryItem = inventoryItem;
        this.productVariantId = inventoryItem.getProductVariantId();
        this.sku = inventoryItem.getSku();
        this.movementType = movementType;
        this.quantity = quantity;
        this.note = note;
        this.referenceType = referenceType;
        this.referenceId = referenceId;
    }

    public InventoryItem getInventoryItem() {
        return inventoryItem;
    }

    public UUID getProductVariantId() {
        return productVariantId;
    }

    public String getSku() {
        return sku;
    }

    public StockMovementType getMovementType() {
        return movementType;
    }

    public int getQuantity() {
        return quantity;
    }

    public String getNote() {
        return note;
    }

    public ReferenceType getReferenceType() {
        return referenceType;
    }

    public String getReferenceId() {
        return referenceId;
    }
}
