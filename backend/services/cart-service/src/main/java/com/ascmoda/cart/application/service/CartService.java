package com.ascmoda.cart.application.service;

import com.ascmoda.cart.api.dto.AddCartItemRequest;
import com.ascmoda.cart.api.dto.CartRefreshResponse;
import com.ascmoda.cart.api.dto.CartResponse;
import com.ascmoda.cart.api.dto.CartSummaryResponse;
import com.ascmoda.cart.api.dto.CartValidationIssueResponse;
import com.ascmoda.cart.api.dto.CartValidationIssueType;
import com.ascmoda.cart.api.dto.CartValidationResponse;
import com.ascmoda.cart.api.dto.CheckoutPreviewResponse;
import com.ascmoda.cart.api.dto.ToggleCartItemSelectionRequest;
import com.ascmoda.cart.api.dto.ToggleCartItemsSelectionRequest;
import com.ascmoda.cart.api.dto.UpdateCartItemQuantityRequest;
import com.ascmoda.cart.api.error.CartItemNotFoundException;
import com.ascmoda.cart.api.error.CartNotFoundException;
import com.ascmoda.cart.api.error.ExternalCatalogException;
import com.ascmoda.cart.api.error.ExternalServiceUnavailableException;
import com.ascmoda.cart.api.error.InsufficientStockException;
import com.ascmoda.cart.application.mapper.CartItemMapper;
import com.ascmoda.cart.application.mapper.CartMapper;
import com.ascmoda.cart.domain.model.Cart;
import com.ascmoda.cart.domain.model.CartItem;
import com.ascmoda.cart.domain.model.CartStatus;
import com.ascmoda.cart.domain.repository.CartRepository;
import com.ascmoda.cart.infrastructure.catalog.CatalogVariantClient;
import com.ascmoda.cart.infrastructure.catalog.CatalogVariantDetailResponse;
import com.ascmoda.cart.infrastructure.inventory.InventoryClient;
import com.ascmoda.cart.infrastructure.inventory.InventoryItemResponse;
import feign.FeignException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

@Service
public class CartService {

    private static final Logger log = LoggerFactory.getLogger(CartService.class);
    private static final String DEFAULT_CURRENCY = "TRY";
    private static final String ACTIVE_PRODUCT_STATUS = "ACTIVE";

    private final CartRepository cartRepository;
    private final CartMapper cartMapper;
    private final CartItemMapper cartItemMapper;
    private final CatalogVariantClient catalogVariantClient;
    private final InventoryClient inventoryClient;

    public CartService(CartRepository cartRepository, CartMapper cartMapper, CartItemMapper cartItemMapper,
                       CatalogVariantClient catalogVariantClient, InventoryClient inventoryClient) {
        this.cartRepository = cartRepository;
        this.cartMapper = cartMapper;
        this.cartItemMapper = cartItemMapper;
        this.catalogVariantClient = catalogVariantClient;
        this.inventoryClient = inventoryClient;
    }

    @Transactional
    public CartResponse getOrCreateActiveCart(UUID customerId) {
        return cartMapper.toResponse(getOrCreateActiveCartEntity(customerId));
    }

    @Transactional
    public CartResponse addItem(UUID customerId, AddCartItemRequest request) {
        Cart cart = getOrCreateActiveCartEntity(customerId);
        cart.ensureActive();

        String sku = normalizeSku(request.sku());
        CatalogVariantDetailResponse variant = getCatalogVariant(request.productVariantId());
        validateCatalogVariantForWrite(variant, sku);

        int targetQuantity = cart.findItemByVariantId(request.productVariantId())
                .map(item -> item.getQuantity() + request.quantity())
                .orElse(request.quantity());
        ensureStockAvailable(request.productVariantId(), targetQuantity);

        CartItem item = cart.findItemByVariantId(request.productVariantId())
                .orElseGet(() -> {
                    CartItem newItem = createCartItem(variant, sku, request.quantity());
                    cart.addItem(newItem);
                    return newItem;
                });

        if (item.getQuantity() != targetQuantity) {
            item.setQuantity(targetQuantity);
        }

        Cart saved = cartRepository.save(cart);
        log.info("Added cart item customerId={} cartId={} productVariantId={} quantity={}",
                customerId, saved.getId(), request.productVariantId(), targetQuantity);
        return cartMapper.toResponse(saved);
    }

    @Transactional
    public CartResponse updateItemQuantity(UUID customerId, UUID itemId, UpdateCartItemQuantityRequest request) {
        Cart cart = getActiveCartEntity(customerId);
        cart.ensureActive();
        CartItem item = getCartItem(cart, itemId);
        ensureStockAvailable(item.getProductVariantId(), request.quantity());
        item.setQuantity(request.quantity());

        log.info("Updated cart item quantity customerId={} cartId={} itemId={} quantity={}",
                customerId, cart.getId(), itemId, request.quantity());
        return cartMapper.toResponse(cart);
    }

    @Transactional
    public CartResponse toggleItemSelection(UUID customerId, UUID itemId, ToggleCartItemSelectionRequest request) {
        Cart cart = getActiveCartEntity(customerId);
        cart.ensureActive();
        CartItem item = getCartItem(cart, itemId);
        item.setSelected(request.selected());

        log.info("Changed cart item selection customerId={} cartId={} itemId={} selected={}",
                customerId, cart.getId(), itemId, request.selected());
        return cartMapper.toResponse(cart);
    }

    @Transactional
    public CartResponse toggleAllItemsSelection(UUID customerId, ToggleCartItemsSelectionRequest request) {
        Cart cart = getActiveCartEntity(customerId);
        cart.ensureActive();
        cart.getItems().forEach(item -> item.setSelected(request.selected()));
        log.info("Changed all cart item selections customerId={} cartId={} selected={}",
                customerId, cart.getId(), request.selected());
        return cartMapper.toResponse(cart);
    }

    @Transactional
    public CartResponse removeItem(UUID customerId, UUID itemId) {
        Cart cart = getActiveCartEntity(customerId);
        cart.ensureActive();
        CartItem item = getCartItem(cart, itemId);
        cart.removeItem(item);

        log.info("Removed cart item customerId={} cartId={} itemId={}", customerId, cart.getId(), itemId);
        return cartMapper.toResponse(cart);
    }

    @Transactional
    public CartResponse clearCart(UUID customerId) {
        Cart cart = getActiveCartEntity(customerId);
        cart.ensureActive();
        cart.clearItems();

        log.info("Cleared cart customerId={} cartId={}", customerId, cart.getId());
        return cartMapper.toResponse(cart);
    }

    @Transactional
    public CartRefreshResponse refresh(UUID customerId) {
        Cart cart = getActiveCartEntity(customerId);
        CartValidationResponse validation = validateCart(cart, false, true);
        log.info("Refreshed cart customerId={} cartId={} issues={}",
                customerId, cart.getId(), validation.issues().size());
        return new CartRefreshResponse(cartMapper.toResponse(cart), validation);
    }

    @Transactional(readOnly = true)
    public CartResponse getActiveCart(UUID customerId) {
        return cartMapper.toResponse(getActiveCartEntity(customerId));
    }

    @Transactional(readOnly = true)
    public CartSummaryResponse getSummary(UUID customerId) {
        return cartMapper.toSummaryResponse(getActiveCartEntity(customerId));
    }

    @Transactional(readOnly = true)
    public CartValidationResponse validateActiveCart(UUID customerId) {
        return validateCart(getActiveCartEntity(customerId), false, false);
    }

    @Transactional(readOnly = true)
    public CheckoutPreviewResponse getCheckoutPreview(UUID customerId) {
        Cart cart = getActiveCartEntity(customerId);
        CartValidationResponse validation = validateCart(cart, true, false);
        return new CheckoutPreviewResponse(
                cart.getId(),
                cart.getCustomerId(),
                cart.getCurrency(),
                cart.getItems()
                        .stream()
                        .filter(CartItem::isSelected)
                        .map(cartItemMapper::toResponse)
                        .toList(),
                cartMapper.selectedItemCount(cart),
                cartMapper.selectedTotal(cart),
                validation
        );
    }

    @Transactional
    public CartResponse markActiveCartCheckedOut(UUID customerId) {
        Cart cart = getActiveCartEntity(customerId);
        cart.markCheckedOut();
        log.info("Marked cart checked out customerId={} cartId={}", customerId, cart.getId());
        return cartMapper.toResponse(cart);
    }

    @Transactional
    public CartResponse markActiveCartAbandoned(UUID customerId) {
        Cart cart = getActiveCartEntity(customerId);
        cart.markAbandoned();
        log.info("Marked cart abandoned customerId={} cartId={}", customerId, cart.getId());
        return cartMapper.toResponse(cart);
    }

    @Transactional(readOnly = true)
    public CartResponse getById(UUID cartId) {
        Cart cart = cartRepository.findWithItemsById(cartId)
                .orElseThrow(() -> new CartNotFoundException("Cart not found: " + cartId));
        return cartMapper.toResponse(cart);
    }

    @Transactional(readOnly = true)
    public Page<CartResponse> listAdmin(CartStatus status, UUID customerId, Instant createdFrom, Instant createdTo,
                                        Pageable pageable) {
        return cartRepository.searchAdmin(status, customerId, createdFrom, createdTo, pageable)
                .map(cartMapper::toResponse);
    }

    private Cart getOrCreateActiveCartEntity(UUID customerId) {
        return cartRepository.findByCustomerIdAndStatus(customerId, CartStatus.ACTIVE)
                .orElseGet(() -> {
                    Cart cart = cartRepository.save(new Cart(customerId, DEFAULT_CURRENCY));
                    log.info("Created active cart customerId={} cartId={}", customerId, cart.getId());
                    return cart;
                });
    }

    private Cart getActiveCartEntity(UUID customerId) {
        return cartRepository.findByCustomerIdAndStatus(customerId, CartStatus.ACTIVE)
                .orElseThrow(() -> new CartNotFoundException("Active cart not found for customer: " + customerId));
    }

    private CartItem getCartItem(Cart cart, UUID itemId) {
        return cart.getItems()
                .stream()
                .filter(item -> item.getId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> new CartItemNotFoundException("Cart item not found: " + itemId));
    }

    private CartValidationResponse validateCart(Cart cart, boolean selectedOnly, boolean refreshSnapshots) {
        List<CartValidationIssueResponse> issues = new ArrayList<>();

        for (CartItem item : cart.getItems()) {
            if (selectedOnly && !item.isSelected()) {
                continue;
            }
            validateItem(item, selectedOnly, refreshSnapshots, issues);
        }

        return new CartValidationResponse(
                cart.getId(),
                cart.getCustomerId(),
                issues.isEmpty(),
                cart.getItems().size(),
                cartMapper.selectedItemCount(cart),
                cartMapper.totalAmount(cart),
                cartMapper.selectedTotal(cart),
                issues
        );
    }

    private void validateItem(CartItem item, boolean selectedOnly, boolean refreshSnapshots,
                              List<CartValidationIssueResponse> issues) {
        CatalogVariantDetailResponse variant = fetchCatalogForValidation(item, issues);
        if (variant != null) {
            validateCatalogForValidation(item, variant, refreshSnapshots, issues);
        }

        if (!selectedOnly || item.isSelected()) {
            validateInventoryForValidation(item, issues);
        }
    }

    private CatalogVariantDetailResponse fetchCatalogForValidation(CartItem item,
                                                                   List<CartValidationIssueResponse> issues) {
        try {
            return catalogVariantClient.getVariantDetails(item.getProductVariantId());
        } catch (FeignException.NotFound ex) {
            issues.add(issue(item, CartValidationIssueType.VARIANT_NOT_FOUND, "Catalog variant was not found"));
        } catch (FeignException ex) {
            issues.add(issue(item, CartValidationIssueType.EXTERNAL_SERVICE_UNAVAILABLE,
                    "Catalog service is unavailable"));
        } catch (RuntimeException ex) {
            issues.add(issue(item, CartValidationIssueType.EXTERNAL_SERVICE_UNAVAILABLE,
                    "Catalog service is unavailable"));
        }
        return null;
    }

    private void validateCatalogForValidation(CartItem item, CatalogVariantDetailResponse variant,
                                              boolean refreshSnapshots,
                                              List<CartValidationIssueResponse> issues) {
        if (!ACTIVE_PRODUCT_STATUS.equals(variant.productStatus())) {
            issues.add(issue(item, CartValidationIssueType.PRODUCT_INACTIVE, "Product is not active"));
        }
        if (!variant.variantActive()) {
            issues.add(issue(item, CartValidationIssueType.VARIANT_INACTIVE, "Product variant is not active"));
        }
        if (!normalizeSku(variant.sku()).equals(item.getSku())) {
            issues.add(issue(item, CartValidationIssueType.SNAPSHOT_OUTDATED, "SKU snapshot is outdated"));
        }
        if (hasPriceChanged(item, variant)) {
            issues.add(issue(item, CartValidationIssueType.PRICE_CHANGED, "Unit price has changed"));
        }
        if (hasSnapshotChanged(item, variant)) {
            issues.add(issue(item, CartValidationIssueType.SNAPSHOT_OUTDATED, "Cart item snapshot is outdated"));
        }
        if (item.getQuantity() < 1) {
            issues.add(issue(item, CartValidationIssueType.INVALID_QUANTITY, "Quantity must be at least 1"));
        }

        if (refreshSnapshots) {
            refreshItemSnapshot(item, variant);
        }
    }

    private void validateInventoryForValidation(CartItem item, List<CartValidationIssueResponse> issues) {
        try {
            InventoryItemResponse inventory = inventoryClient.getByProductVariantId(item.getProductVariantId());
            if (!inventory.active()) {
                issues.add(issue(item, CartValidationIssueType.INSUFFICIENT_STOCK, "Inventory item is inactive"));
            } else if (inventory.availableQuantity() < item.getQuantity()) {
                issues.add(issue(item, CartValidationIssueType.INSUFFICIENT_STOCK, "Insufficient available stock"));
            }
        } catch (FeignException.NotFound ex) {
            issues.add(issue(item, CartValidationIssueType.INSUFFICIENT_STOCK, "Inventory item was not found"));
        } catch (FeignException ex) {
            issues.add(issue(item, CartValidationIssueType.EXTERNAL_SERVICE_UNAVAILABLE,
                    "Inventory service is unavailable"));
        } catch (RuntimeException ex) {
            issues.add(issue(item, CartValidationIssueType.EXTERNAL_SERVICE_UNAVAILABLE,
                    "Inventory service is unavailable"));
        }
    }

    private CartValidationIssueResponse issue(CartItem item, CartValidationIssueType type, String message) {
        return new CartValidationIssueResponse(
                item.getId(),
                item.getProductVariantId(),
                item.getSku(),
                type,
                message
        );
    }

    private CatalogVariantDetailResponse getCatalogVariant(UUID productVariantId) {
        try {
            return catalogVariantClient.getVariantDetails(productVariantId);
        } catch (FeignException.NotFound ex) {
            throw new ExternalCatalogException("Catalog product variant not found: " + productVariantId);
        } catch (FeignException ex) {
            throw new ExternalServiceUnavailableException("Catalog service is unavailable");
        } catch (RuntimeException ex) {
            throw new ExternalServiceUnavailableException("Catalog service is unavailable");
        }
    }

    private void validateCatalogVariantForWrite(CatalogVariantDetailResponse variant, String requestedSku) {
        if (!normalizeSku(variant.sku()).equals(requestedSku)) {
            throw new ExternalCatalogException("Cart item SKU must match catalog variant SKU");
        }
        if (!ACTIVE_PRODUCT_STATUS.equals(variant.productStatus())) {
            throw new ExternalCatalogException("Inactive product cannot be added to cart");
        }
        if (!variant.variantActive()) {
            throw new ExternalCatalogException("Inactive product variant cannot be added to cart");
        }
        if (variant.effectiveUnitPrice() == null || variant.effectiveUnitPrice().compareTo(BigDecimal.ZERO) < 0) {
            throw new ExternalCatalogException("Catalog variant price is not valid");
        }
    }

    private void ensureStockAvailable(UUID productVariantId, int requestedQuantity) {
        try {
            InventoryItemResponse inventory = inventoryClient.getByProductVariantId(productVariantId);
            if (!inventory.active()) {
                throw new InsufficientStockException("Inventory item is inactive");
            }
            if (inventory.availableQuantity() < requestedQuantity) {
                throw new InsufficientStockException("Insufficient stock for product variant: " + productVariantId);
            }
        } catch (InsufficientStockException ex) {
            throw ex;
        } catch (FeignException.NotFound ex) {
            throw new InsufficientStockException("Inventory item not found for product variant: " + productVariantId);
        } catch (FeignException ex) {
            throw new ExternalServiceUnavailableException("Inventory service is unavailable");
        } catch (RuntimeException ex) {
            throw new ExternalServiceUnavailableException("Inventory service is unavailable");
        }
    }

    private CartItem createCartItem(CatalogVariantDetailResponse variant, String sku, int quantity) {
        return new CartItem(
                variant.productId(),
                variant.id(),
                sku,
                variant.productName(),
                variant.productSlug(),
                variantName(variant),
                variant.mainImageUrl(),
                variant.color(),
                variant.size(),
                variant.effectiveUnitPrice(),
                quantity
        );
    }

    private void refreshItemSnapshot(CartItem item, CatalogVariantDetailResponse variant) {
        item.refreshSnapshot(
                variant.productId(),
                normalizeSku(variant.sku()),
                variant.productName(),
                variant.productSlug(),
                variantName(variant),
                variant.mainImageUrl(),
                variant.color(),
                variant.size(),
                variant.effectiveUnitPrice()
        );
    }

    private boolean hasPriceChanged(CartItem item, CatalogVariantDetailResponse variant) {
        return variant.effectiveUnitPrice() != null
                && item.getUnitPriceSnapshot().compareTo(variant.effectiveUnitPrice()) != 0;
    }

    private boolean hasSnapshotChanged(CartItem item, CatalogVariantDetailResponse variant) {
        return !Objects.equals(item.getProductId(), variant.productId())
                || !Objects.equals(item.getProductNameSnapshot(), variant.productName())
                || !Objects.equals(item.getProductSlugSnapshot(), variant.productSlug())
                || !Objects.equals(item.getVariantNameSnapshot(), variantName(variant))
                || !Objects.equals(item.getMainImageUrlSnapshot(), variant.mainImageUrl())
                || !Objects.equals(item.getColorSnapshot(), variant.color())
                || !Objects.equals(item.getSizeSnapshot(), variant.size());
    }

    private String variantName(CatalogVariantDetailResponse variant) {
        if (variant.color() == null && variant.size() == null) {
            return null;
        }
        if (variant.color() == null) {
            return variant.size();
        }
        if (variant.size() == null) {
            return variant.color();
        }
        return variant.color() + " / " + variant.size();
    }

    private String normalizeSku(String sku) {
        if (sku == null || sku.isBlank()) {
            throw new IllegalArgumentException("SKU must be provided");
        }
        return sku.trim().toUpperCase(Locale.ROOT);
    }
}
