package com.ascmoda.order.domain.model;

import com.ascmoda.order.domain.exception.InvalidOrderStateException;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.hibernate.annotations.BatchSize;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Entity
@BatchSize(size = 50)
@Table(
        name = "orders",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_orders_order_number", columnNames = "order_number"),
                @UniqueConstraint(name = "uk_orders_source_cart_id", columnNames = "source_cart_id")
        },
        indexes = {
                @Index(name = "idx_orders_customer_id", columnList = "customer_id"),
                @Index(name = "idx_orders_status", columnList = "status"),
                @Index(name = "idx_orders_created_at", columnList = "created_at"),
                @Index(name = "idx_orders_order_number", columnList = "order_number")
        }
)
public class Order extends BaseAuditableEntity {

    @Column(name = "order_number", nullable = false, length = 40)
    private String orderNumber;

    @Column(name = "source_cart_id", nullable = false)
    private UUID sourceCartId;

    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private OrderStatus status = OrderStatus.PENDING;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(name = "subtotal_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotalAmount = BigDecimal.ZERO;

    @Column(name = "discount_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Column(name = "shipping_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal shippingAmount = BigDecimal.ZERO;

    @Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Column(length = 1000)
    private String note;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private OrderSource source = OrderSource.WEB;

    @Embedded
    private AddressSnapshot shippingAddress;

    @Embedded
    private CustomerSnapshot customerSnapshot;

    @Column(name = "placed_at", nullable = false)
    private Instant placedAt;

    @Column(name = "confirmed_at")
    private Instant confirmedAt;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @Column(name = "cancellation_reason", length = 500)
    private String cancellationReason;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @BatchSize(size = 50)
    private List<OrderItem> items = new ArrayList<>();

    protected Order() {
    }

    public Order(String orderNumber, UUID sourceCartId, UUID customerId, String currency, String note,
                 OrderSource source, AddressSnapshot shippingAddress, CustomerSnapshot customerSnapshot) {
        this.orderNumber = requireText(orderNumber, "Order number must be provided");
        this.sourceCartId = requireUuid(sourceCartId, "Source cart id must be provided");
        this.customerId = requireUuid(customerId, "Customer id must be provided");
        this.currency = currency == null || currency.isBlank() ? "TRY" : currency.trim().toUpperCase();
        this.note = normalizeOptional(note);
        this.source = source == null ? OrderSource.WEB : source;
        this.shippingAddress = shippingAddress;
        this.customerSnapshot = customerSnapshot;
        this.status = OrderStatus.PENDING;
        this.placedAt = Instant.now();
    }

    public void addItem(OrderItem item) {
        ensurePending("Order items can only be added while order is pending");
        item.setOrder(this);
        items.add(item);
        recalculateTotals();
    }

    public void confirm() {
        ensurePending("Only pending orders can be confirmed");
        status = OrderStatus.CONFIRMED;
        confirmedAt = Instant.now();
    }

    public void cancel(String reason) {
        ensurePending("Only pending orders can be cancelled");
        status = OrderStatus.CANCELLED;
        cancelledAt = Instant.now();
        cancellationReason = requireText(reason, "Cancellation reason must be provided");
    }

    public void markFailed(String reason) {
        ensurePending("Only pending orders can be marked as failed");
        status = OrderStatus.FAILED;
        cancellationReason = normalizeOptional(reason);
    }

    public void ensurePending(String message) {
        if (status != OrderStatus.PENDING) {
            throw new InvalidOrderStateException(message);
        }
    }

    private void recalculateTotals() {
        subtotalAmount = items.stream()
                .map(OrderItem::getLineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        totalAmount = subtotalAmount.subtract(discountAmount).add(shippingAmount);
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

    public String getOrderNumber() {
        return orderNumber;
    }

    public UUID getSourceCartId() {
        return sourceCartId;
    }

    public UUID getCustomerId() {
        return customerId;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public String getCurrency() {
        return currency;
    }

    public BigDecimal getSubtotalAmount() {
        return subtotalAmount;
    }

    public BigDecimal getDiscountAmount() {
        return discountAmount;
    }

    public BigDecimal getShippingAmount() {
        return shippingAmount;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public String getNote() {
        return note;
    }

    public OrderSource getSource() {
        return source;
    }

    public AddressSnapshot getShippingAddress() {
        return shippingAddress;
    }

    public CustomerSnapshot getCustomerSnapshot() {
        return customerSnapshot;
    }

    public Instant getPlacedAt() {
        return placedAt;
    }

    public Instant getConfirmedAt() {
        return confirmedAt;
    }

    public Instant getCancelledAt() {
        return cancelledAt;
    }

    public String getCancellationReason() {
        return cancellationReason;
    }

    public List<OrderItem> getItems() {
        return Collections.unmodifiableList(items);
    }
}
