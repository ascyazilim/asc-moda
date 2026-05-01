package com.ascmoda.order.domain.model;

import com.ascmoda.order.domain.exception.InvalidOrderStateException;
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

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@BatchSize(size = 50)
@Table(
        name = "order_items",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_order_items_order_variant", columnNames = {"order_id", "product_variant_id"})
        },
        indexes = {
                @Index(name = "idx_order_items_order_id", columnList = "order_id"),
                @Index(name = "idx_order_items_product_variant_id", columnList = "product_variant_id"),
                @Index(name = "idx_order_items_sku", columnList = "sku"),
                @Index(name = "idx_order_items_reservation_id", columnList = "reservation_id")
        }
)
public class OrderItem extends BaseAuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Column(name = "cart_item_id", nullable = false)
    private UUID cartItemId;

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Column(name = "product_variant_id", nullable = false)
    private UUID productVariantId;

    @Column(nullable = false, length = 120)
    private String sku;

    @Column(name = "product_name_snapshot", nullable = false, length = 220)
    private String productNameSnapshot;

    @Column(name = "product_slug_snapshot", nullable = false, length = 240)
    private String productSlugSnapshot;

    @Column(name = "main_image_url_snapshot", length = 1000)
    private String mainImageUrlSnapshot;

    @Column(name = "color_snapshot", length = 80)
    private String colorSnapshot;

    @Column(name = "size_snapshot", length = 40)
    private String sizeSnapshot;

    @Column(name = "unit_price_snapshot", nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPriceSnapshot;

    @Column(nullable = false)
    private int quantity;

    @Column(name = "line_total", nullable = false, precision = 12, scale = 2)
    private BigDecimal lineTotal;

    @Column(name = "reservation_id")
    private UUID reservationId;

    @Column(name = "reservation_key", nullable = false, length = 160)
    private String reservationKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "reservation_status", nullable = false, length = 30)
    private OrderReservationStatus reservationStatus = OrderReservationStatus.ACTIVE;

    protected OrderItem() {
    }

    public OrderItem(UUID cartItemId, UUID productId, UUID productVariantId, String sku, String productNameSnapshot,
                     String productSlugSnapshot, String mainImageUrlSnapshot, String colorSnapshot, String sizeSnapshot,
                     BigDecimal unitPriceSnapshot, int quantity, UUID reservationId, String reservationKey) {
        this.cartItemId = requireUuid(cartItemId, "Cart item id must be provided");
        this.productId = requireUuid(productId, "Product id must be provided");
        this.productVariantId = requireUuid(productVariantId, "Product variant id must be provided");
        this.sku = requireText(sku, "SKU must be provided");
        this.productNameSnapshot = requireText(productNameSnapshot, "Product name snapshot must be provided");
        this.productSlugSnapshot = requireText(productSlugSnapshot, "Product slug snapshot must be provided");
        this.mainImageUrlSnapshot = normalizeOptional(mainImageUrlSnapshot);
        this.colorSnapshot = normalizeOptional(colorSnapshot);
        this.sizeSnapshot = normalizeOptional(sizeSnapshot);
        this.unitPriceSnapshot = requireMoney(unitPriceSnapshot, "Unit price snapshot must be provided");
        this.quantity = requirePositive(quantity);
        this.lineTotal = this.unitPriceSnapshot.multiply(BigDecimal.valueOf(quantity));
        this.reservationId = reservationId;
        this.reservationKey = requireText(reservationKey, "Reservation key must be provided");
        this.reservationStatus = OrderReservationStatus.ACTIVE;
    }

    void setOrder(Order order) {
        this.order = order;
    }

    public void markReservationConsumed() {
        if (reservationStatus == OrderReservationStatus.RELEASED) {
            throw new InvalidOrderStateException("Released reservation cannot be consumed");
        }
        reservationStatus = OrderReservationStatus.CONSUMED;
    }

    public void markReservationReleased() {
        if (reservationStatus == OrderReservationStatus.CONSUMED) {
            throw new InvalidOrderStateException("Consumed reservation cannot be released");
        }
        reservationStatus = OrderReservationStatus.RELEASED;
    }

    public boolean hasActiveReservation() {
        return reservationStatus == OrderReservationStatus.ACTIVE;
    }

    private UUID requireUuid(UUID value, String message) {
        if (value == null) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }

    private String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }

    private String normalizeOptional(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private BigDecimal requireMoney(BigDecimal value, String message) {
        if (value == null) {
            throw new IllegalArgumentException(message);
        }
        if (value.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Money amount cannot be negative");
        }
        return value;
    }

    private int requirePositive(int value) {
        if (value < 1) {
            throw new IllegalArgumentException("Order item quantity must be at least 1");
        }
        return value;
    }

    public Order getOrder() {
        return order;
    }

    public UUID getCartItemId() {
        return cartItemId;
    }

    public UUID getProductId() {
        return productId;
    }

    public UUID getProductVariantId() {
        return productVariantId;
    }

    public String getSku() {
        return sku;
    }

    public String getProductNameSnapshot() {
        return productNameSnapshot;
    }

    public String getProductSlugSnapshot() {
        return productSlugSnapshot;
    }

    public String getMainImageUrlSnapshot() {
        return mainImageUrlSnapshot;
    }

    public String getColorSnapshot() {
        return colorSnapshot;
    }

    public String getSizeSnapshot() {
        return sizeSnapshot;
    }

    public BigDecimal getUnitPriceSnapshot() {
        return unitPriceSnapshot;
    }

    public int getQuantity() {
        return quantity;
    }

    public BigDecimal getLineTotal() {
        return lineTotal;
    }

    public UUID getReservationId() {
        return reservationId;
    }

    public String getReservationKey() {
        return reservationKey;
    }

    public OrderReservationStatus getReservationStatus() {
        return reservationStatus;
    }
}
