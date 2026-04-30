package com.ascmoda.cart.domain.model;

import com.ascmoda.cart.api.error.InvalidCartStateException;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import org.hibernate.annotations.BatchSize;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Entity
@BatchSize(size = 50)
@Table(
        name = "carts",
        indexes = {
                @Index(name = "idx_carts_customer_id", columnList = "customer_id"),
                @Index(name = "idx_carts_status", columnList = "status")
        }
)
public class Cart extends BaseAuditableEntity {

    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private CartStatus status = CartStatus.ACTIVE;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(name = "last_activity_at", nullable = false)
    private Instant lastActivityAt;

    @Column(name = "checked_out_at")
    private Instant checkedOutAt;

    @Column(name = "abandoned_at")
    private Instant abandonedAt;

    @OneToMany(mappedBy = "cart", cascade = CascadeType.ALL, orphanRemoval = true)
    @BatchSize(size = 50)
    private List<CartItem> items = new ArrayList<>();

    protected Cart() {
    }

    public Cart(UUID customerId, String currency) {
        this.customerId = customerId;
        this.currency = currency;
        this.status = CartStatus.ACTIVE;
        this.lastActivityAt = Instant.now();
    }

    public void addItem(CartItem item) {
        ensureActive();
        item.setCart(this);
        items.add(item);
        touch();
    }

    public void removeItem(CartItem item) {
        ensureActive();
        items.remove(item);
        item.setCart(null);
        touch();
    }

    public void clearItems() {
        ensureActive();
        items.forEach(item -> item.setCart(null));
        items.clear();
        touch();
    }

    public void touch() {
        lastActivityAt = Instant.now();
    }

    public void markCheckedOut() {
        ensureActive();
        status = CartStatus.CHECKED_OUT;
        checkedOutAt = Instant.now();
        touch();
    }

    public void markAbandoned() {
        ensureActive();
        status = CartStatus.ABANDONED;
        abandonedAt = Instant.now();
        touch();
    }

    public void ensureActive() {
        if (status != CartStatus.ACTIVE) {
            throw new InvalidCartStateException("Cart is not active");
        }
    }

    public Optional<CartItem> findItemByVariantId(UUID productVariantId) {
        return items.stream()
                .filter(item -> item.getProductVariantId().equals(productVariantId))
                .findFirst();
    }

    public UUID getCustomerId() {
        return customerId;
    }

    public CartStatus getStatus() {
        return status;
    }

    public String getCurrency() {
        return currency;
    }

    public Instant getLastActivityAt() {
        return lastActivityAt;
    }

    public Instant getCheckedOutAt() {
        return checkedOutAt;
    }

    public Instant getAbandonedAt() {
        return abandonedAt;
    }

    public List<CartItem> getItems() {
        return items;
    }
}
