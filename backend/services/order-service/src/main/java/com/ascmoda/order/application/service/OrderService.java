package com.ascmoda.order.application.service;

import com.ascmoda.order.application.mapper.OrderMapper;
import com.ascmoda.order.controller.dto.CancelOrderRequest;
import com.ascmoda.order.controller.dto.CreateOrderRequest;
import com.ascmoda.order.controller.dto.OrderListItemResponse;
import com.ascmoda.order.controller.dto.OrderResponse;
import com.ascmoda.order.controller.dto.OrderSummaryResponse;
import com.ascmoda.order.controller.dto.PageResponse;
import com.ascmoda.order.domain.exception.CartNotReadyException;
import com.ascmoda.order.domain.exception.CheckoutPreviewInvalidException;
import com.ascmoda.order.domain.exception.EmptyCheckoutSelectionException;
import com.ascmoda.order.domain.exception.ExternalServiceUnavailableException;
import com.ascmoda.order.domain.exception.InventoryConsumeFailedException;
import com.ascmoda.order.domain.exception.InventoryReleaseFailedException;
import com.ascmoda.order.domain.exception.InventoryReservationFailedException;
import com.ascmoda.order.domain.exception.InvalidOrderStateException;
import com.ascmoda.order.domain.exception.OrderNotFoundException;
import com.ascmoda.order.domain.model.AddressSnapshot;
import com.ascmoda.order.domain.model.CustomerSnapshot;
import com.ascmoda.order.domain.model.Order;
import com.ascmoda.order.domain.model.OrderItem;
import com.ascmoda.order.domain.model.OrderReservationStatus;
import com.ascmoda.order.domain.model.OrderSource;
import com.ascmoda.order.domain.model.OrderStatus;
import com.ascmoda.order.domain.repository.OrderRepository;
import com.ascmoda.order.infrastructure.cart.CartClient;
import com.ascmoda.order.infrastructure.cart.CartItemResponse;
import com.ascmoda.order.infrastructure.cart.CartValidationIssueResponse;
import com.ascmoda.order.infrastructure.cart.CheckoutPreviewResponse;
import com.ascmoda.order.infrastructure.inventory.ConsumeStockReservationRequest;
import com.ascmoda.order.infrastructure.inventory.InventoryClient;
import com.ascmoda.order.infrastructure.inventory.ReferenceType;
import com.ascmoda.order.infrastructure.inventory.ReleaseStockRequest;
import com.ascmoda.order.infrastructure.inventory.ReserveStockRequest;
import com.ascmoda.order.infrastructure.inventory.StockReservationResponse;
import com.ascmoda.order.infrastructure.inventory.StockReservationStatus;
import feign.FeignException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);
    private static final int ORDER_NUMBER_ATTEMPTS = 6;

    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;
    private final CartClient cartClient;
    private final InventoryClient inventoryClient;
    private final OrderNumberGenerator orderNumberGenerator;
    private final OrderOutboxService orderOutboxService;
    private final long reservationTtlMinutes;

    public OrderService(OrderRepository orderRepository, OrderMapper orderMapper, CartClient cartClient,
                        InventoryClient inventoryClient, OrderNumberGenerator orderNumberGenerator,
                        OrderOutboxService orderOutboxService,
                        @Value("${ascmoda.order.reservation-ttl-minutes:30}") long reservationTtlMinutes) {
        this.orderRepository = orderRepository;
        this.orderMapper = orderMapper;
        this.cartClient = cartClient;
        this.inventoryClient = inventoryClient;
        this.orderNumberGenerator = orderNumberGenerator;
        this.orderOutboxService = orderOutboxService;
        this.reservationTtlMinutes = reservationTtlMinutes;
    }

    @Transactional
    public OrderResponse createOrder(CreateOrderRequest request) {
        Optional<Order> existingForIdempotencyKey = findExistingOrderForIdempotencyKey(request);
        if (existingForIdempotencyKey.isPresent()) {
            Order existing = existingForIdempotencyKey.get();
            log.info("Returned existing order for idempotencyKey customerId={} orderId={} orderNumber={}",
                    request.customerId(), existing.getId(), existing.getOrderNumber());
            return orderMapper.toResponse(existing);
        }

        CheckoutPreviewResponse preview = fetchCheckoutPreview(request.customerId());
        validateCheckoutPreview(request, preview);
        Optional<Order> existingForCart = orderRepository.findWithItemsBySourceCartId(preview.cartId());
        if (existingForCart.isPresent()) {
            Order existing = existingForCart.get();
            log.info("Returned existing order for sourceCartId={} orderId={} orderNumber={}",
                    preview.cartId(), existing.getId(), existing.getOrderNumber());
            return orderMapper.toResponse(existing);
        }

        String orderNumber = nextOrderNumber();
        List<StockReservationResponse> reservations = new ArrayList<>();

        try {
            for (CartItemResponse item : preview.selectedItems()) {
                reservations.add(reserveItem(orderNumber, item));
            }

            Order order = buildOrder(request, preview, orderNumber, reservations);
            Order saved = orderRepository.saveAndFlush(order);
            orderOutboxService.recordOrderCreated(saved);
            markCartCheckedOut(request.customerId());

            log.info("Created order id={} orderNumber={} customerId={} cartId={}",
                    saved.getId(), saved.getOrderNumber(), saved.getCustomerId(), saved.getSourceCartId());
            return orderMapper.toResponse(saved);
        } catch (RuntimeException ex) {
            log.info("Order creation failed after {} reservation(s), releasing reserved stock orderNumber={}",
                    reservations.size(), orderNumber);
            releaseReservationsAfterCreateFailure(reservations, orderNumber);
            throw ex;
        }
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrder(UUID orderId) {
        return orderMapper.toResponse(getOrderWithItems(orderId));
    }

    @Transactional(readOnly = true)
    public PageResponse<OrderSummaryResponse> listCustomerOrders(UUID customerId, Pageable pageable) {
        Page<OrderSummaryResponse> page = orderRepository.findByCustomerId(customerId, pageable)
                .map(orderMapper::toSummaryResponse);
        return orderMapper.toPageResponse(page);
    }

    @Transactional(readOnly = true)
    public PageResponse<OrderListItemResponse> listAdmin(OrderStatus status, UUID customerId, String orderNumber,
                                                         Instant createdFrom, Instant createdTo,
                                                         BigDecimal totalMin, BigDecimal totalMax, Pageable pageable) {
        validateTotalRange(totalMin, totalMax);
        Page<OrderListItemResponse> page = orderRepository.findAll(
                        adminSpecification(status, customerId, orderNumber, createdFrom, createdTo, totalMin, totalMax),
                        pageable
                )
                .map(orderMapper::toListItemResponse);
        return orderMapper.toPageResponse(page);
    }

    @Transactional(noRollbackFor = {
            ExternalServiceUnavailableException.class,
            InventoryConsumeFailedException.class,
            InventoryReservationFailedException.class
    })
    public OrderResponse confirm(UUID orderId) {
        Order order = getOrderWithItems(orderId);
        order.ensurePending("Only pending orders can be confirmed");

        for (OrderItem item : order.getItems()) {
            if (item.hasActiveReservation()) {
                consumeReservation(item);
                item.markReservationConsumed();
            }
        }

        order.confirm();
        orderOutboxService.recordOrderConfirmed(order);
        log.info("Confirmed order id={} orderNumber={}", order.getId(), order.getOrderNumber());
        return orderMapper.toResponse(order);
    }

    @Transactional(noRollbackFor = {
            ExternalServiceUnavailableException.class,
            InventoryReleaseFailedException.class,
            InventoryReservationFailedException.class
    })
    public OrderResponse cancel(UUID orderId, CancelOrderRequest request) {
        Order order = getOrderWithItems(orderId);
        order.ensurePending("Only pending orders can be cancelled");

        for (OrderItem item : order.getItems()) {
            if (item.getReservationStatus() == OrderReservationStatus.CONSUMED) {
                throw new InvalidOrderStateException("Order with consumed reservation cannot be cancelled");
            }
            if (item.hasActiveReservation()) {
                releaseReservation(order, item, "Order cancellation");
                item.markReservationReleased();
            }
        }

        order.cancel(request.reason());
        orderOutboxService.recordOrderCancelled(order);
        log.info("Cancelled order id={} orderNumber={}", order.getId(), order.getOrderNumber());
        return orderMapper.toResponse(order);
    }

    private Order getOrderWithItems(UUID orderId) {
        return orderRepository.findWithItemsById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order not found: " + orderId));
    }

    private CheckoutPreviewResponse fetchCheckoutPreview(UUID customerId) {
        try {
            return cartClient.getCheckoutPreview(customerId);
        } catch (FeignException.NotFound ex) {
            throw new CartNotReadyException("Active cart not found for customer: " + customerId);
        } catch (FeignException.UnprocessableEntity ex) {
            throw new CartNotReadyException("Cart is not ready for checkout");
        } catch (FeignException ex) {
            throw new ExternalServiceUnavailableException("Cart service is unavailable");
        } catch (RuntimeException ex) {
            if (ex instanceof CartNotReadyException || ex instanceof CheckoutPreviewInvalidException) {
                throw ex;
            }
            throw new ExternalServiceUnavailableException("Cart service is unavailable");
        }
    }

    private void validateCheckoutPreview(CreateOrderRequest request, CheckoutPreviewResponse preview) {
        if (preview == null || preview.cartId() == null) {
            throw new CheckoutPreviewInvalidException("Checkout preview is not valid");
        }
        if (!request.customerId().equals(preview.customerId())) {
            throw new CheckoutPreviewInvalidException("Checkout preview customer does not match request customer");
        }
        if (preview.selectedItems() == null || preview.selectedItems().isEmpty()) {
            throw new EmptyCheckoutSelectionException("Checkout requires at least one selected cart item");
        }
        if (preview.selectedTotal() == null) {
            throw new CheckoutPreviewInvalidException("Checkout selected total is not valid");
        }
        if (preview.validation() == null || !preview.validation().valid()) {
            throw new CheckoutPreviewInvalidException(checkoutInvalidMessage(preview));
        }
        if (preview.selectedItemCount() != preview.selectedItems().size()) {
            throw new CheckoutPreviewInvalidException("Checkout selected item count does not match selected items");
        }

        BigDecimal itemTotal = preview.selectedItems()
                .stream()
                .map(this::validateCartItem)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (itemTotal.compareTo(preview.selectedTotal()) != 0) {
            throw new CheckoutPreviewInvalidException("Checkout selected total does not match item totals");
        }
    }

    private BigDecimal validateCartItem(CartItemResponse item) {
        if (item.id() == null || item.productId() == null || item.productVariantId() == null) {
            throw new CheckoutPreviewInvalidException("Checkout item identity is not valid");
        }
        if (item.sku() == null || item.sku().isBlank()) {
            throw new CheckoutPreviewInvalidException("Checkout item SKU is not valid");
        }
        if (item.productNameSnapshot() == null || item.productNameSnapshot().isBlank()) {
            throw new CheckoutPreviewInvalidException("Checkout item product snapshot is not valid");
        }
        if (item.productSlugSnapshot() == null || item.productSlugSnapshot().isBlank()) {
            throw new CheckoutPreviewInvalidException("Checkout item product slug snapshot is not valid");
        }
        if (item.quantity() < 1) {
            throw new CheckoutPreviewInvalidException("Checkout item quantity is not valid");
        }
        if (item.unitPriceSnapshot() == null || item.unitPriceSnapshot().compareTo(BigDecimal.ZERO) < 0) {
            throw new CheckoutPreviewInvalidException("Checkout item unit price is not valid");
        }

        BigDecimal expectedLineTotal = item.unitPriceSnapshot().multiply(BigDecimal.valueOf(item.quantity()));
        if (item.lineTotal() == null || item.lineTotal().compareTo(expectedLineTotal) != 0) {
            throw new CheckoutPreviewInvalidException("Checkout item line total is not valid");
        }
        return item.lineTotal();
    }

    private String checkoutInvalidMessage(CheckoutPreviewResponse preview) {
        List<CartValidationIssueResponse> issues = preview.validation().issues();
        if (issues == null || issues.isEmpty()) {
            return "Checkout preview is invalid";
        }
        return "Checkout preview is invalid: " + issues.stream()
                .map(issue -> issue.sku() + " - " + issue.message())
                .collect(Collectors.joining("; "));
    }

    private Optional<Order> findExistingOrderForIdempotencyKey(CreateOrderRequest request) {
        String idempotencyKey = normalizeOptional(request.idempotencyKey());
        if (idempotencyKey == null) {
            return Optional.empty();
        }
        return orderRepository.findWithItemsByCustomerIdAndIdempotencyKey(request.customerId(), idempotencyKey);
    }

    private String nextOrderNumber() {
        for (int i = 0; i < ORDER_NUMBER_ATTEMPTS; i++) {
            String candidate = orderNumberGenerator.generate();
            if (!orderRepository.existsByOrderNumber(candidate)) {
                return candidate;
            }
        }
        throw new IllegalStateException("Could not generate a unique order number");
    }

    private StockReservationResponse reserveItem(String orderNumber, CartItemResponse item) {
        String reservationKey = reservationKey(orderNumber, item);
        try {
            StockReservationResponse reservation = inventoryClient.reserve(new ReserveStockRequest(
                    null,
                    item.productVariantId(),
                    item.sku(),
                    item.quantity(),
                    "Order checkout reservation",
                    ReferenceType.ORDER,
                    orderNumber,
                    reservationKey,
                    Instant.now().plus(reservationTtlMinutes, ChronoUnit.MINUTES)
            ));
            if (reservation.status() != StockReservationStatus.ACTIVE || reservation.quantity() != item.quantity()) {
                throw new InventoryReservationFailedException("Inventory reservation did not return an active reservation");
            }
            return reservation;
        } catch (FeignException.UnprocessableEntity ex) {
            throw new InventoryReservationFailedException("Inventory reservation failed for SKU: " + item.sku());
        } catch (FeignException.NotFound ex) {
            throw new InventoryReservationFailedException("Inventory item was not found for SKU: " + item.sku());
        } catch (FeignException ex) {
            throw new ExternalServiceUnavailableException("Inventory service is unavailable");
        } catch (RuntimeException ex) {
            if (ex instanceof InventoryReservationFailedException || ex instanceof ExternalServiceUnavailableException) {
                throw ex;
            }
            throw new ExternalServiceUnavailableException("Inventory service is unavailable");
        }
    }

    private Order buildOrder(CreateOrderRequest request, CheckoutPreviewResponse preview, String orderNumber,
                             List<StockReservationResponse> reservations) {
        Map<UUID, StockReservationResponse> reservationsByVariant = reservations.stream()
                .collect(Collectors.toMap(StockReservationResponse::productVariantId, Function.identity()));
        AddressSnapshot shippingAddress = orderMapper.toAddressSnapshot(request.shippingAddress());
        CustomerSnapshot customerSnapshot = orderMapper.toCustomerSnapshot(request);
        Order order = new Order(
                orderNumber,
                preview.cartId(),
                request.customerId(),
                preview.currency(),
                request.note(),
                resolveSource(request.source()),
                shippingAddress,
                customerSnapshot,
                normalizeOptional(request.idempotencyKey()),
                normalizeOptional(request.externalReference())
        );

        for (CartItemResponse item : preview.selectedItems()) {
            StockReservationResponse reservation = reservationsByVariant.get(item.productVariantId());
            if (reservation == null) {
                throw new InventoryReservationFailedException("Missing inventory reservation for SKU: " + item.sku());
            }
            order.addItem(new OrderItem(
                    item.id(),
                    item.productId(),
                    item.productVariantId(),
                    normalizeSku(item.sku()),
                    item.productNameSnapshot(),
                    item.productSlugSnapshot(),
                    item.mainImageUrlSnapshot(),
                    item.colorSnapshot(),
                    item.sizeSnapshot(),
                    item.unitPriceSnapshot(),
                    item.quantity(),
                    reservation.id(),
                    reservation.reservationKey()
            ));
        }

        if (order.getTotalAmount().compareTo(preview.selectedTotal()) != 0) {
            throw new CheckoutPreviewInvalidException("Order total does not match checkout preview total");
        }
        return order;
    }

    private OrderSource resolveSource(OrderSource source) {
        return source == null ? OrderSource.WEB : source;
    }

    private void markCartCheckedOut(UUID customerId) {
        try {
            cartClient.markCheckedOut(customerId);
        } catch (FeignException.NotFound ex) {
            throw new CartNotReadyException("Active cart not found for customer: " + customerId);
        } catch (FeignException.UnprocessableEntity ex) {
            throw new CartNotReadyException("Cart cannot be marked checked out");
        } catch (FeignException ex) {
            throw new ExternalServiceUnavailableException("Cart service is unavailable");
        } catch (RuntimeException ex) {
            if (ex instanceof CartNotReadyException) {
                throw ex;
            }
            throw new ExternalServiceUnavailableException("Cart service is unavailable");
        }
    }

    private void releaseReservationsAfterCreateFailure(List<StockReservationResponse> reservations, String orderNumber) {
        for (StockReservationResponse reservation : reservations) {
            try {
                inventoryClient.release(new ReleaseStockRequest(
                        reservation.id(),
                        reservation.reservationKey(),
                        null,
                        null,
                        null,
                        reservation.quantity(),
                        "Compensating release after order creation failed",
                        ReferenceType.ORDER,
                        orderNumber
                ));
            } catch (RuntimeException releaseFailure) {
                log.warn("Failed to release reservation after order creation failure reservationId={} key={}",
                        reservation.id(), reservation.reservationKey());
            }
        }
    }

    private void consumeReservation(OrderItem item) {
        try {
            inventoryClient.consume(new ConsumeStockReservationRequest(
                    item.getReservationId(),
                    item.getReservationKey(),
                    item.getQuantity(),
                    "Order confirmation"
            ));
        } catch (FeignException.UnprocessableEntity ex) {
            throw new InventoryConsumeFailedException("Inventory reservation cannot be consumed for SKU: " + item.getSku());
        } catch (FeignException.NotFound ex) {
            throw new InventoryConsumeFailedException("Inventory reservation was not found for SKU: " + item.getSku());
        } catch (FeignException ex) {
            throw new ExternalServiceUnavailableException("Inventory service is unavailable");
        } catch (RuntimeException ex) {
            if (ex instanceof InventoryConsumeFailedException || ex instanceof ExternalServiceUnavailableException) {
                throw ex;
            }
            throw new ExternalServiceUnavailableException("Inventory service is unavailable");
        }
    }

    private void releaseReservation(Order order, OrderItem item, String note) {
        try {
            inventoryClient.release(new ReleaseStockRequest(
                    item.getReservationId(),
                    item.getReservationKey(),
                    null,
                    null,
                    null,
                    item.getQuantity(),
                    note,
                    ReferenceType.ORDER,
                    order.getOrderNumber()
            ));
        } catch (FeignException.UnprocessableEntity ex) {
            throw new InventoryReleaseFailedException("Inventory reservation cannot be released for SKU: " + item.getSku());
        } catch (FeignException.NotFound ex) {
            throw new InventoryReleaseFailedException("Inventory reservation was not found for SKU: " + item.getSku());
        } catch (FeignException ex) {
            throw new ExternalServiceUnavailableException("Inventory service is unavailable");
        } catch (RuntimeException ex) {
            if (ex instanceof InventoryReleaseFailedException || ex instanceof ExternalServiceUnavailableException) {
                throw ex;
            }
            throw new ExternalServiceUnavailableException("Inventory service is unavailable");
        }
    }

    private Specification<Order> adminSpecification(OrderStatus status, UUID customerId, String orderNumber,
                                                    Instant createdFrom, Instant createdTo,
                                                    BigDecimal totalMin, BigDecimal totalMax) {
        Specification<Order> specification = (root, query, criteriaBuilder) -> criteriaBuilder.conjunction();
        if (status != null) {
            specification = specification.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.equal(root.get("status"), status));
        }
        if (customerId != null) {
            specification = specification.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.equal(root.get("customerId"), customerId));
        }
        String normalizedOrderNumber = normalizeSearch(orderNumber);
        if (!normalizedOrderNumber.isBlank()) {
            specification = specification.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("orderNumber")), "%" + normalizedOrderNumber + "%"));
        }
        if (createdFrom != null) {
            specification = specification.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.greaterThanOrEqualTo(root.get("createdAt"), createdFrom));
        }
        if (createdTo != null) {
            specification = specification.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.lessThanOrEqualTo(root.get("createdAt"), createdTo));
        }
        if (totalMin != null) {
            specification = specification.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.greaterThanOrEqualTo(root.get("totalAmount"), totalMin));
        }
        if (totalMax != null) {
            specification = specification.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.lessThanOrEqualTo(root.get("totalAmount"), totalMax));
        }
        return specification;
    }

    private void validateTotalRange(BigDecimal totalMin, BigDecimal totalMax) {
        if (totalMin != null && totalMin.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("totalMin cannot be negative");
        }
        if (totalMax != null && totalMax.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("totalMax cannot be negative");
        }
        if (totalMin != null && totalMax != null && totalMin.compareTo(totalMax) > 0) {
            throw new IllegalArgumentException("totalMin cannot be greater than totalMax");
        }
    }

    private String reservationKey(String orderNumber, CartItemResponse item) {
        return "ORDER:" + orderNumber + ":" + item.id();
    }

    private String normalizeSku(String sku) {
        return sku.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeSearch(String value) {
        return value == null || value.isBlank() ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeOptional(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
