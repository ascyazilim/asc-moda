package com.ascmoda.inventory.domain.model;

import com.ascmoda.inventory.api.error.InvalidReservationStateException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.hibernate.annotations.BatchSize;

import java.time.Instant;
import java.util.UUID;

@Entity
@BatchSize(size = 50)
@Table(
        name = "stock_reservations",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_stock_reservations_key", columnNames = "reservation_key")
        },
        indexes = {
                @Index(name = "idx_stock_reservations_inventory_item_id", columnList = "inventory_item_id"),
                @Index(name = "idx_stock_reservations_product_variant_id", columnList = "product_variant_id"),
                @Index(name = "idx_stock_reservations_sku", columnList = "sku"),
                @Index(name = "idx_stock_reservations_status", columnList = "status"),
                @Index(name = "idx_stock_reservations_reference", columnList = "reference_type,reference_id"),
                @Index(name = "idx_stock_reservations_expires_at", columnList = "expires_at")
        }
)
public class StockReservation extends BaseAuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "inventory_item_id", nullable = false)
    private InventoryItem inventoryItem;

    @Column(name = "product_variant_id", nullable = false)
    private UUID productVariantId;

    @Column(nullable = false, length = 120)
    private String sku;

    @Enumerated(EnumType.STRING)
    @Column(name = "reference_type", nullable = false, length = 30)
    private ReferenceType referenceType;

    @Column(name = "reference_id", nullable = false, length = 120)
    private String referenceId;

    @Column(name = "reservation_key", nullable = false, length = 160)
    private String reservationKey;

    @Column(nullable = false)
    private int quantity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private StockReservationStatus status = StockReservationStatus.ACTIVE;

    @Column(name = "expires_at")
    private Instant expiresAt;

    protected StockReservation() {
    }

    public StockReservation(InventoryItem inventoryItem, ReferenceType referenceType, String referenceId,
                            String reservationKey, int quantity, Instant expiresAt) {
        this.inventoryItem = inventoryItem;
        this.productVariantId = inventoryItem.getProductVariantId();
        this.sku = inventoryItem.getSku();
        this.referenceType = referenceType;
        this.referenceId = referenceId;
        this.reservationKey = reservationKey;
        this.quantity = quantity;
        this.expiresAt = expiresAt;
        requirePositive(quantity);
    }

    public int release(int requestedQuantity) {
        ensureActive();
        int quantityToRelease = resolveOperationQuantity(requestedQuantity);
        quantity -= quantityToRelease;
        if (quantity == 0) {
            status = StockReservationStatus.RELEASED;
        }
        return quantityToRelease;
    }

    public int consume(int requestedQuantity) {
        ensureActive();
        int quantityToConsume = resolveOperationQuantity(requestedQuantity);
        quantity -= quantityToConsume;
        if (quantity == 0) {
            status = StockReservationStatus.CONSUMED;
        }
        return quantityToConsume;
    }

    public void cancel() {
        ensureActive();
        status = StockReservationStatus.CANCELLED;
    }

    public void expire(Instant now) {
        ensureActive();
        if (expiresAt == null || expiresAt.isAfter(now)) {
            throw new InvalidReservationStateException("Reservation is not expired");
        }
        status = StockReservationStatus.EXPIRED;
    }

    public boolean isActive() {
        return status == StockReservationStatus.ACTIVE;
    }

    public void ensureActive() {
        if (!isActive()) {
            throw new InvalidReservationStateException("Reservation is not active");
        }
    }

    public boolean matches(InventoryItem item, int requestedQuantity) {
        return inventoryItem.getId().equals(item.getId()) && quantity == requestedQuantity && isActive();
    }

    private int resolveOperationQuantity(int requestedQuantity) {
        requirePositive(requestedQuantity);
        if (requestedQuantity > quantity) {
            throw new InvalidReservationStateException("Reservation operation quantity cannot exceed active quantity");
        }
        return requestedQuantity;
    }

    private void requirePositive(int quantity) {
        if (quantity <= 0) {
            throw new InvalidReservationStateException("Reservation quantity must be greater than zero");
        }
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

    public ReferenceType getReferenceType() {
        return referenceType;
    }

    public String getReferenceId() {
        return referenceId;
    }

    public String getReservationKey() {
        return reservationKey;
    }

    public int getQuantity() {
        return quantity;
    }

    public StockReservationStatus getStatus() {
        return status;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }
}
