package com.ascmoda.inventory.application.service;

import com.ascmoda.inventory.api.dto.AdjustStockRequest;
import com.ascmoda.inventory.api.dto.CreateInventoryItemRequest;
import com.ascmoda.inventory.api.dto.InventoryItemResponse;
import com.ascmoda.inventory.api.dto.ReleaseStockRequest;
import com.ascmoda.inventory.api.dto.ReserveStockRequest;
import com.ascmoda.inventory.api.dto.StockMovementResponse;
import com.ascmoda.inventory.api.dto.UpdateInventoryItemRequest;
import com.ascmoda.inventory.api.error.DuplicateInventoryItemException;
import com.ascmoda.inventory.api.error.ExternalCatalogValidationException;
import com.ascmoda.inventory.api.error.InvalidStockStateException;
import com.ascmoda.inventory.api.error.ResourceNotFoundException;
import com.ascmoda.inventory.application.mapper.InventoryItemMapper;
import com.ascmoda.inventory.application.mapper.StockMovementMapper;
import com.ascmoda.inventory.domain.model.InventoryItem;
import com.ascmoda.inventory.domain.model.ReferenceType;
import com.ascmoda.inventory.domain.model.StockMovement;
import com.ascmoda.inventory.domain.model.StockMovementType;
import com.ascmoda.inventory.domain.repository.InventoryItemRepository;
import com.ascmoda.inventory.domain.repository.StockMovementRepository;
import com.ascmoda.inventory.infrastructure.catalog.CatalogVariantClient;
import com.ascmoda.inventory.infrastructure.catalog.CatalogVariantResponse;
import feign.FeignException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.UUID;

@Service
public class InventoryService {

    private static final Logger log = LoggerFactory.getLogger(InventoryService.class);

    private final InventoryItemRepository inventoryItemRepository;
    private final StockMovementRepository stockMovementRepository;
    private final InventoryItemMapper inventoryItemMapper;
    private final StockMovementMapper stockMovementMapper;
    private final CatalogVariantClient catalogVariantClient;

    public InventoryService(InventoryItemRepository inventoryItemRepository,
                            StockMovementRepository stockMovementRepository,
                            InventoryItemMapper inventoryItemMapper,
                            StockMovementMapper stockMovementMapper,
                            CatalogVariantClient catalogVariantClient) {
        this.inventoryItemRepository = inventoryItemRepository;
        this.stockMovementRepository = stockMovementRepository;
        this.inventoryItemMapper = inventoryItemMapper;
        this.stockMovementMapper = stockMovementMapper;
        this.catalogVariantClient = catalogVariantClient;
    }

    @Transactional
    public InventoryItemResponse create(CreateInventoryItemRequest request) {
        String sku = normalizeSku(request.sku());
        validateCatalogVariant(request.productVariantId(), sku);
        ensureSkuAvailable(sku, null);
        ensureProductVariantAvailable(request.productVariantId(), null);

        InventoryItem item = new InventoryItem(
                request.productVariantId(),
                sku,
                request.quantityOnHand(),
                request.reservedQuantity(),
                request.active() == null || request.active()
        );

        InventoryItem saved = inventoryItemRepository.save(item);
        recordInitialMovements(saved);
        log.info("Created inventory item id={} productVariantId={} sku={}",
                saved.getId(), saved.getProductVariantId(), saved.getSku());
        return inventoryItemMapper.toResponse(saved);
    }

    @Transactional
    public InventoryItemResponse update(UUID id, UpdateInventoryItemRequest request) {
        InventoryItem item = getItem(id);
        String sku = normalizeSku(request.sku());
        validateCatalogVariant(request.productVariantId(), sku);
        ensureSkuAvailable(sku, id);
        ensureProductVariantAvailable(request.productVariantId(), id);

        int previousQuantityOnHand = item.getQuantityOnHand();
        int previousReservedQuantity = item.getReservedQuantity();

        item.update(
                request.productVariantId(),
                sku,
                request.quantityOnHand(),
                request.reservedQuantity(),
                request.active() == null || request.active()
        );

        recordUpdateMovements(item, previousQuantityOnHand, previousReservedQuantity);
        log.info("Updated inventory item id={} productVariantId={} sku={}",
                item.getId(), item.getProductVariantId(), item.getSku());
        return inventoryItemMapper.toResponse(item);
    }

    @Transactional
    public InventoryItemResponse adjustStock(AdjustStockRequest request) {
        return switch (request.movementType()) {
            case INCREASE -> increaseStock(request);
            case DECREASE -> decreaseStock(request);
            case ADJUSTMENT -> adjustQuantityOnHand(request);
            case RESERVE, RELEASE -> throw new InvalidStockStateException(
                    "Use reserve or release endpoints for reservation movements"
            );
        };
    }

    @Transactional
    public InventoryItemResponse increaseStock(AdjustStockRequest request) {
        InventoryItem item = resolveItem(request.inventoryItemId(), request.sku(), request.productVariantId());
        item.increase(request.quantity());
        recordMovement(item, StockMovementType.INCREASE, request.quantity(), request.note(),
                defaultReferenceType(request.referenceType()), request.referenceId());
        log.info("Increased inventory item id={} sku={} quantity={}", item.getId(), item.getSku(), request.quantity());
        return inventoryItemMapper.toResponse(item);
    }

    @Transactional
    public InventoryItemResponse decreaseStock(AdjustStockRequest request) {
        InventoryItem item = resolveItem(request.inventoryItemId(), request.sku(), request.productVariantId());
        item.decrease(request.quantity());
        recordMovement(item, StockMovementType.DECREASE, request.quantity(), request.note(),
                defaultReferenceType(request.referenceType()), request.referenceId());
        log.info("Decreased inventory item id={} sku={} quantity={}", item.getId(), item.getSku(), request.quantity());
        return inventoryItemMapper.toResponse(item);
    }

    @Transactional
    public InventoryItemResponse reserve(ReserveStockRequest request) {
        InventoryItem item = resolveItem(request.inventoryItemId(), request.sku(), request.productVariantId());
        item.reserve(request.quantity());
        recordMovement(item, StockMovementType.RESERVE, request.quantity(), request.note(),
                defaultReferenceType(request.referenceType()), request.referenceId());
        log.info("Reserved inventory item id={} sku={} quantity={}", item.getId(), item.getSku(), request.quantity());
        return inventoryItemMapper.toResponse(item);
    }

    @Transactional
    public InventoryItemResponse release(ReleaseStockRequest request) {
        InventoryItem item = resolveItem(request.inventoryItemId(), request.sku(), request.productVariantId());
        item.release(request.quantity());
        recordMovement(item, StockMovementType.RELEASE, request.quantity(), request.note(),
                defaultReferenceType(request.referenceType()), request.referenceId());
        log.info("Released inventory item id={} sku={} quantity={}", item.getId(), item.getSku(), request.quantity());
        return inventoryItemMapper.toResponse(item);
    }

    @Transactional(readOnly = true)
    public InventoryItemResponse getBySku(String sku) {
        return inventoryItemMapper.toResponse(findBySku(sku));
    }

    @Transactional(readOnly = true)
    public InventoryItemResponse getByProductVariantId(UUID productVariantId) {
        return inventoryItemMapper.toResponse(findByProductVariantId(productVariantId));
    }

    @Transactional(readOnly = true)
    public InventoryItemResponse getById(UUID id) {
        return inventoryItemMapper.toResponse(getItem(id));
    }

    @Transactional(readOnly = true)
    public Page<InventoryItemResponse> listAdmin(Boolean active, String sku, Pageable pageable) {
        return inventoryItemRepository.searchAdmin(active, normalizeSearch(sku), pageable)
                .map(inventoryItemMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<InventoryItemResponse> listLowStock(int threshold, Pageable pageable) {
        if (threshold < 0) {
            throw new InvalidStockStateException("Low stock threshold cannot be negative");
        }
        return inventoryItemRepository.findLowStock(threshold, pageable)
                .map(inventoryItemMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<StockMovementResponse> listMovements(UUID inventoryItemId, String sku, Pageable pageable) {
        if (inventoryItemId != null) {
            return stockMovementRepository.findByInventoryItemIdOrderByCreatedAtDesc(inventoryItemId, pageable)
                    .map(stockMovementMapper::toResponse);
        }
        if (sku != null && !sku.isBlank()) {
            return stockMovementRepository.findBySkuOrderByCreatedAtDesc(normalizeSku(sku), pageable)
                    .map(stockMovementMapper::toResponse);
        }
        return stockMovementRepository.findAllByOrderByCreatedAtDesc(pageable)
                .map(stockMovementMapper::toResponse);
    }

    private InventoryItemResponse adjustQuantityOnHand(AdjustStockRequest request) {
        InventoryItem item = resolveItem(request.inventoryItemId(), request.sku(), request.productVariantId());
        int targetQuantity = request.quantity();
        int delta = targetQuantity - item.getQuantityOnHand();

        if (delta == 0) {
            throw new InvalidStockStateException("Adjustment must change quantity on hand");
        }
        if (targetQuantity < item.getReservedQuantity()) {
            throw new InvalidStockStateException("Adjusted quantity cannot be lower than reserved quantity");
        }

        item.update(item.getProductVariantId(), item.getSku(), targetQuantity, item.getReservedQuantity(), item.isActive());
        recordMovement(item, StockMovementType.ADJUSTMENT, Math.abs(delta), request.note(),
                defaultReferenceType(request.referenceType()), request.referenceId());
        log.info("Adjusted inventory item id={} sku={} targetQuantity={}",
                item.getId(), item.getSku(), targetQuantity);
        return inventoryItemMapper.toResponse(item);
    }

    private void recordInitialMovements(InventoryItem item) {
        if (item.getQuantityOnHand() > 0) {
            recordMovement(item, StockMovementType.ADJUSTMENT, item.getQuantityOnHand(), "Initial stock",
                    ReferenceType.ADMIN, null);
        }
        if (item.getReservedQuantity() > 0) {
            recordMovement(item, StockMovementType.RESERVE, item.getReservedQuantity(), "Initial reservation",
                    ReferenceType.ADMIN, null);
        }
    }

    private void recordUpdateMovements(InventoryItem item, int previousQuantityOnHand, int previousReservedQuantity) {
        int quantityDelta = item.getQuantityOnHand() - previousQuantityOnHand;
        int reservedDelta = item.getReservedQuantity() - previousReservedQuantity;

        if (quantityDelta != 0) {
            recordMovement(item, StockMovementType.ADJUSTMENT, Math.abs(quantityDelta), "Admin inventory update",
                    ReferenceType.ADMIN, null);
        }
        if (reservedDelta > 0) {
            recordMovement(item, StockMovementType.RESERVE, reservedDelta, "Admin reservation update",
                    ReferenceType.ADMIN, null);
        }
        if (reservedDelta < 0) {
            recordMovement(item, StockMovementType.RELEASE, Math.abs(reservedDelta), "Admin reservation update",
                    ReferenceType.ADMIN, null);
        }
    }

    private void recordMovement(InventoryItem item, StockMovementType movementType, int quantity, String note,
                                ReferenceType referenceType, String referenceId) {
        stockMovementRepository.save(new StockMovement(item, movementType, quantity, note, referenceType, referenceId));
    }

    private InventoryItem resolveItem(UUID id, String sku, UUID productVariantId) {
        if (id != null) {
            return getItem(id);
        }
        if (sku != null && !sku.isBlank()) {
            return findBySku(sku);
        }
        if (productVariantId != null) {
            return findByProductVariantId(productVariantId);
        }
        throw new IllegalArgumentException("inventoryItemId, sku or productVariantId must be provided");
    }

    private InventoryItem getItem(UUID id) {
        return inventoryItemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory item not found: " + id));
    }

    private InventoryItem findBySku(String sku) {
        return inventoryItemRepository.findBySku(normalizeSku(sku))
                .orElseThrow(() -> new ResourceNotFoundException("Inventory item not found for SKU: " + sku));
    }

    private InventoryItem findByProductVariantId(UUID productVariantId) {
        return inventoryItemRepository.findByProductVariantId(productVariantId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Inventory item not found for product variant: " + productVariantId
                ));
    }

    private void ensureSkuAvailable(String sku, UUID currentId) {
        boolean exists = currentId == null
                ? inventoryItemRepository.existsBySku(sku)
                : inventoryItemRepository.existsBySkuAndIdNot(sku, currentId);
        if (exists) {
            throw new DuplicateInventoryItemException("Inventory SKU already exists: " + sku);
        }
    }

    private void ensureProductVariantAvailable(UUID productVariantId, UUID currentId) {
        boolean exists = currentId == null
                ? inventoryItemRepository.existsByProductVariantId(productVariantId)
                : inventoryItemRepository.existsByProductVariantIdAndIdNot(productVariantId, currentId);
        if (exists) {
            throw new DuplicateInventoryItemException(
                    "Inventory item already exists for product variant: " + productVariantId
            );
        }
    }

    private void validateCatalogVariant(UUID productVariantId, String sku) {
        try {
            CatalogVariantResponse variant = catalogVariantClient.getVariant(productVariantId);
            if (!variant.sku().equals(sku)) {
                throw new ExternalCatalogValidationException("Inventory SKU must match catalog variant SKU");
            }
        } catch (FeignException.NotFound ex) {
            throw new ExternalCatalogValidationException("Catalog product variant not found: " + productVariantId);
        } catch (FeignException ex) {
            throw new ExternalCatalogValidationException("Catalog variant validation failed");
        }
    }

    private ReferenceType defaultReferenceType(ReferenceType referenceType) {
        return referenceType == null ? ReferenceType.ADMIN : referenceType;
    }

    private String normalizeSku(String sku) {
        if (sku == null || sku.isBlank()) {
            throw new IllegalArgumentException("SKU must be provided");
        }
        return sku.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeSearch(String value) {
        return value == null || value.isBlank() ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
