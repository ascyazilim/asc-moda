package com.ascmoda.cart.domain.model;

import com.ascmoda.cart.api.error.InvalidCartStateException;
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
import java.util.UUID;

@Entity
@BatchSize(size = 50)
@Table(
        name = "cart_items",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_cart_items_cart_variant", columnNames = {"cart_id", "product_variant_id"})
        },
        indexes = {
                @Index(name = "idx_cart_items_cart_id", columnList = "cart_id"),
                @Index(name = "idx_cart_items_product_variant_id", columnList = "product_variant_id"),
                @Index(name = "idx_cart_items_sku", columnList = "sku")
        }
)
public class CartItem extends BaseAuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cart_id", nullable = false)
    private Cart cart;

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

    @Column(name = "variant_name_snapshot", length = 160)
    private String variantNameSnapshot;

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

    @Column(name = "is_selected", nullable = false)
    private boolean selected = true;

    protected CartItem() {
    }

    public CartItem(UUID productId, UUID productVariantId, String sku, String productNameSnapshot,
                    String productSlugSnapshot, String variantNameSnapshot, String mainImageUrlSnapshot,
                    String colorSnapshot, String sizeSnapshot,
                    BigDecimal unitPriceSnapshot, int quantity) {
        this.productId = productId;
        this.productVariantId = productVariantId;
        this.sku = sku;
        this.productNameSnapshot = productNameSnapshot;
        this.productSlugSnapshot = productSlugSnapshot;
        this.variantNameSnapshot = variantNameSnapshot;
        this.mainImageUrlSnapshot = mainImageUrlSnapshot;
        this.colorSnapshot = colorSnapshot;
        this.sizeSnapshot = sizeSnapshot;
        this.unitPriceSnapshot = unitPriceSnapshot;
        setQuantity(quantity);
    }

    public void increaseQuantity(int amount) {
        setQuantity(quantity + amount);
    }

    public void setQuantity(int quantity) {
        if (quantity < 1) {
            throw new InvalidCartStateException("Cart item quantity must be at least 1");
        }
        this.quantity = quantity;
        touchCart();
    }

    public void refreshSnapshot(UUID productId, String sku, String productNameSnapshot, String productSlugSnapshot,
                                String variantNameSnapshot, String mainImageUrlSnapshot, String colorSnapshot,
                                String sizeSnapshot, BigDecimal unitPriceSnapshot) {
        this.productId = productId;
        this.sku = sku;
        this.productNameSnapshot = productNameSnapshot;
        this.productSlugSnapshot = productSlugSnapshot;
        this.variantNameSnapshot = variantNameSnapshot;
        this.mainImageUrlSnapshot = mainImageUrlSnapshot;
        this.colorSnapshot = colorSnapshot;
        this.sizeSnapshot = sizeSnapshot;
        this.unitPriceSnapshot = unitPriceSnapshot;
        touchCart();
    }

    public BigDecimal lineTotal() {
        return unitPriceSnapshot.multiply(BigDecimal.valueOf(quantity));
    }

    public Cart getCart() {
        return cart;
    }

    public void setCart(Cart cart) {
        this.cart = cart;
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

    public String getVariantNameSnapshot() {
        return variantNameSnapshot;
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

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
        touchCart();
    }

    private void touchCart() {
        if (cart != null) {
            cart.touch();
        }
    }
}
