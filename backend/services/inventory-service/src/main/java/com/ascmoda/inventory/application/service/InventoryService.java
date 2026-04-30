package com.ascmoda.inventory.application.service;

import com.ascmoda.inventory.api.dto.AdjustStockRequest;
import com.ascmoda.inventory.api.dto.AvailabilityResponse;
import com.ascmoda.inventory.api.dto.ConsumeStockReservationRequest;
import com.ascmoda.inventory.api.dto.CreateInventoryItemRequest;
import com.ascmoda.inventory.api.dto.InventoryItemResponse;
import com.ascmoda.inventory.api.dto.InventorySummaryResponse;
import com.ascmoda.inventory.api.dto.ReleaseStockRequest;
import com.ascmoda.inventory.api.dto.ReserveStockRequest;
import com.ascmoda.inventory.api.dto.StockMovementResponse;
import com.ascmoda.inventory.api.dto.StockReservationResponse;
import com.ascmoda.inventory.api.dto.UpdateInventoryItemRequest;
import com.ascmoda.inventory.api.dto.ValidateStockRequest;
import com.ascmoda.inventory.api.error.DuplicateInventoryItemException;
import com.ascmoda.inventory.api.error.ExternalCatalogValidationException;
import com.ascmoda.inventory.api.error.ExternalServiceUnavailableException;
import com.ascmoda.inventory.api.error.InvalidReservationStateException;
import com.ascmoda.inventory.api.error.InvalidStockStateException;
import com.ascmoda.inventory.api.error.ReservationNotFoundException;
import com.ascmoda.inventory.api.error.ResourceNotFoundException;
import com.ascmoda.inventory.application.mapper.InventoryItemMapper;
import com.ascmoda.inventory.application.mapper.StockMovementMapper;
import com.ascmoda.inventory.application.mapper.StockReservationMapper;
import com.ascmoda.inventory.domain.model.InventoryItem;
import com.ascmoda.inventory.domain.model.ReferenceType;
import com.ascmoda.inventory.domain.model.StockMovement;
import com.ascmoda.inventory.domain.model.StockMovementType;
import com.ascmoda.inventory.domain.model.StockReservation;
import com.ascmoda.inventory.domain.model.StockReservationStatus;
import com.ascmoda.inventory.domain.repository.InventoryItemRepository;
import com.ascmoda.inventory.domain.repository.StockMovementRepository;
import com.ascmoda.inventory.domain.repository.StockReservationRepository;
import com.ascmoda.inventory.infrastructure.catalog.CatalogVariantClient;
import com.ascmoda.inventory.infrastructure.catalog.CatalogVariantResponse;
import feign.FeignException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

@Service
public class InventoryService {

    private static final Logger log = LoggerFactory.getLogger(InventoryService.class);
    private static final String ACTIVE_PRODUCT_STATUS = "ACTIVE";
    private static final int DEFAULT_LOW_STOCK_THRESHOLD = 5;

    private final InventoryItemRepository inventoryItemRepository;
    private final StockMovementRepository stockMovementRepository;
    private final StockReservationRepository stockReservationRepository;
    private final InventoryItemMapper inventoryItemMapper;
    private final StockMovementMapper stockMovementMapper;
    private final StockReservationMapper stockReservationMapper;
    private final CatalogVariantClient catalogVariantClient;

    public InventoryService(InventoryItemRepository inventoryItemRepository,
                            StockMovementRepository stockMovementRepository,
                            StockReservationRepository stockReservationRepository,
                            InventoryItemMapper inventoryItemMapper,
                            StockMovementMapper stockMovementMapper,
                            StockReservationMapper stockReservationMapper,
                            CatalogVariantClient catalogVariantClient) {
        this.inventoryItemRepository = inventoryItemRepository;
        this.stockMovementRepository = stockMovementRepository;
        this.stockReservationRepository = stockReservationRepository;
        this.inventoryItemMapper = inventoryItemMapper;
        this.stockMovementMapper = stockMovementMapper;
        this.stockReservationMapper = stockReservationMapper;
        this.catalogVariantClient = catalogVariantClient;
    }

    @Transactional
    public InventoryItemResponse create(CreateInventoryItemRequest request) {
        String sku = normalizeSku(request.sku());
        validateCatalogVariant(request.productVariantId(), sku);
        ensureSkuAvailable(sku, null);
        ensureProductVariantAvailable(request.productVariantId(), null);
        ensureNoInitialReservation(request.reservedQuantity());

        InventoryItem item = new InventoryItem(
                request.productVariantId(),
                sku,
                request.quantityOnHand(),
                0,
                request.active() == null || request.active(),
                lowStockThreshold(request.lowStockThreshold())
        );

        InventoryItem saved = inventoryItemRepository.save(item);
        recordInitialMovement(saved);
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
        ensureReservedQuantityUnchanged(item, request.reservedQuantity());

        int beforeQuantityOnHand = item.getQuantityOnHand();
        int beforeReservedQuantity = item.getReservedQuantity();

        item.update(
                request.productVariantId(),
                sku,
                request.quantityOnHand(),
                item.getReservedQuantity(),
                request.active() == null || request.active(),
                lowStockThreshold(request.lowStockThreshold())
        );

        if (beforeQuantityOnHand != item.getQuantityOnHand()) {
            recordMovement(item, StockMovementType.ADJUSTMENT, Math.abs(item.getQuantityOnHand() - beforeQuantityOnHand),
                    "Admin inventory update", ReferenceType.ADMIN, null,
                    beforeQuantityOnHand, item.getQuantityOnHand(),
                    beforeReservedQuantity, item.getReservedQuantity());
        }
        log.info("Updated inventory item id={} productVariantId={} sku={}",
                item.getId(), item.getProductVariantId(), item.getSku());
        return inventoryItemMapper.toResponse(item);
    }

    @Transactional
    public InventoryItemResponse activate(UUID id) {
        InventoryItem item = getItem(id);
        item.activate();
        log.info("Activated inventory item id={} sku={}", item.getId(), item.getSku());
        return inventoryItemMapper.toResponse(item);
    }

    @Transactional
    public InventoryItemResponse deactivate(UUID id) {
        InventoryItem item = getItem(id);
        item.deactivate();
        log.info("Deactivated inventory item id={} sku={}", item.getId(), item.getSku());
        return inventoryItemMapper.toResponse(item);
    }

    @Transactional
    public InventoryItemResponse adjustStock(AdjustStockRequest request) {
        return switch (request.movementType()) {
            case INCREASE -> increaseStock(request);
            case DECREASE -> decreaseStock(request);
            case ADJUSTMENT -> adjustQuantityOnHand(request);
            case RESERVE, RELEASE, CONSUME -> throw new InvalidStockStateException(
                    "Use reservation endpoints for reservation movements"
            );
        };
    }

    @Transactional
    public InventoryItemResponse increaseStock(AdjustStockRequest request) {
        InventoryItem item = resolveItem(request.inventoryItemId(), request.sku(), request.productVariantId());
        int beforeQuantityOnHand = item.getQuantityOnHand();
        int beforeReservedQuantity = item.getReservedQuantity();

        item.increase(request.quantity());
        recordMovement(item, StockMovementType.INCREASE, request.quantity(), request.note(),
                defaultReferenceType(request.referenceType(), ReferenceType.ADMIN), request.referenceId(),
                beforeQuantityOnHand, item.getQuantityOnHand(), beforeReservedQuantity, item.getReservedQuantity());
        log.info("Increased inventory item id={} sku={} quantity={}", item.getId(), item.getSku(), request.quantity());
        return inventoryItemMapper.toResponse(item);
    }

    @Transactional
    public InventoryItemResponse decreaseStock(AdjustStockRequest request) {
        InventoryItem item = resolveItem(request.inventoryItemId(), request.sku(), request.productVariantId());
        int beforeQuantityOnHand = item.getQuantityOnHand();
        int beforeReservedQuantity = item.getReservedQuantity();

        item.decrease(request.quantity());
        recordMovement(item, StockMovementType.DECREASE, request.quantity(), request.note(),
                defaultReferenceType(request.referenceType(), ReferenceType.ADMIN), request.referenceId(),
                beforeQuantityOnHand, item.getQuantityOnHand(), beforeReservedQuantity, item.getReservedQuantity());
        log.info("Decreased inventory item id={} sku={} quantity={}", item.getId(), item.getSku(), request.quantity());
        return inventoryItemMapper.toResponse(item);
    }

    @Transactional
    public StockReservationResponse reserve(ReserveStockRequest request) {
        InventoryItem item = resolveItem(request.inventoryItemId(), request.sku(), request.productVariantId());
        item.ensureActive();
        ReferenceType referenceType = defaultReferenceType(request.referenceType(), ReferenceType.ORDER);
        String referenceId = requiredReferenceId(request.referenceId());
        String reservationKey = reservationKey(request.reservationKey(), referenceType, referenceId, item.getProductVariantId());

        StockReservation existingReservation = stockReservationRepository.findByReservationKey(reservationKey)
                .orElse(null);
        if (existingReservation != null) {
            return handleDuplicateReserve(existingReservation, item, request.quantity());
        }

        int beforeQuantityOnHand = item.getQuantityOnHand();
        int beforeReservedQuantity = item.getReservedQuantity();

        item.reserve(request.quantity());
        StockReservation reservation = stockReservationRepository.save(new StockReservation(
                item,
                referenceType,
                referenceId,
                reservationKey,
                request.quantity(),
                request.expiresAt()
        ));
        recordMovement(item, StockMovementType.RESERVE, request.quantity(), request.note(),
                referenceType, referenceId, beforeQuantityOnHand, item.getQuantityOnHand(),
                beforeReservedQuantity, item.getReservedQuantity());
        log.info("Reserved stock inventoryItemId={} reservationId={} sku={} quantity={}",
                item.getId(), reservation.getId(), item.getSku(), request.quantity());
        return stockReservationMapper.toResponse(reservation);
    }

    @Transactional
    public StockReservationResponse release(ReleaseStockRequest request) {
        StockReservation reservation = getReservationForOperation(request.reservationId(), request.reservationKey());
        InventoryItem item = reservation.getInventoryItem();
        int quantity = operationQuantity(request.quantity(), reservation);
        int beforeQuantityOnHand = item.getQuantityOnHand();
        int beforeReservedQuantity = item.getReservedQuantity();

        int releasedQuantity = reservation.release(quantity);
        item.release(releasedQuantity);
        recordMovement(item, StockMovementType.RELEASE, releasedQuantity, request.note(),
                reservation.getReferenceType(), reservation.getReferenceId(),
                beforeQuantityOnHand, item.getQuantityOnHand(), beforeReservedQuantity, item.getReservedQuantity());
        log.info("Released reservation reservationId={} inventoryItemId={} quantity={}",
                reservation.getId(), item.getId(), releasedQuantity);
        return stockReservationMapper.toResponse(reservation);
    }

    @Transactional
    public StockReservationResponse consume(ConsumeStockReservationRequest request) {
        StockReservation reservation = getReservationForOperation(request.reservationId(), request.reservationKey());
        InventoryItem item = reservation.getInventoryItem();
        int quantity = operationQuantity(request.quantity(), reservation);
        int beforeQuantityOnHand = item.getQuantityOnHand();
        int beforeReservedQuantity = item.getReservedQuantity();

        int consumedQuantity = reservation.consume(quantity);
        item.consumeReserved(consumedQuantity);
        recordMovement(item, StockMovementType.CONSUME, consumedQuantity, request.note(),
                reservation.getReferenceType(), reservation.getReferenceId(),
                beforeQuantityOnHand, item.getQuantityOnHand(), beforeReservedQuantity, item.getReservedQuantity());
        log.info("Consumed reservation reservationId={} inventoryItemId={} quantity={}",
                reservation.getId(), item.getId(), consumedQuantity);
        return stockReservationMapper.toResponse(reservation);
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
    public AvailabilityResponse getAvailabilityByProductVariantId(UUID productVariantId) {
        return toAvailability(findByProductVariantId(productVariantId), 0);
    }

    @Transactional(readOnly = true)
    public AvailabilityResponse validateStock(ValidateStockRequest request) {
        InventoryItem item = resolveItem(null, request.sku(), request.productVariantId());
        return toAvailability(item, request.quantity());
    }

    @Transactional(readOnly = true)
    public InventoryItemResponse getById(UUID id) {
        return inventoryItemMapper.toResponse(getItem(id));
    }

    @Transactional(readOnly = true)
    public StockReservationResponse getReservation(UUID reservationId) {
        return stockReservationMapper.toResponse(getReservationEntity(reservationId));
    }

    @Transactional(readOnly = true)
    public Page<InventoryItemResponse> listAdmin(Boolean active, String sku, UUID productVariantId,
                                                 Integer lowStockThreshold, Pageable pageable) {
        validateThreshold(lowStockThreshold);
        return inventoryItemRepository.searchAdmin(active, normalizeSearch(sku), productVariantId, lowStockThreshold, pageable)
                .map(inventoryItemMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<InventoryItemResponse> listLowStock(int threshold, Pageable pageable) {
        validateThreshold(threshold);
        return inventoryItemRepository.findLowStock(threshold, pageable)
                .map(inventoryItemMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<StockMovementResponse> listMovements(UUID inventoryItemId, String sku, StockMovementType movementType,
                                                     ReferenceType referenceType, String referenceId,
                                                     Instant createdFrom, Instant createdTo, Pageable pageable) {
        return stockMovementRepository.findAll(
                        stockMovementSpecification(
                                inventoryItemId,
                                normalizeSearch(sku),
                                movementType,
                                referenceType,
                                normalizeExact(referenceId),
                                createdFrom,
                                createdTo
                        ),
                        pageable
                )
                .map(stockMovementMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<StockMovementResponse> listMovements(UUID inventoryItemId, String sku, Pageable pageable) {
        return listMovements(inventoryItemId, sku, null, null, null, null, null, pageable);
    }

    @Transactional(readOnly = true)
    public Page<StockReservationResponse> listReservations(StockReservationStatus status, UUID inventoryItemId,
                                                           UUID productVariantId, String sku,
                                                           ReferenceType referenceType, String referenceId,
                                                           Instant createdFrom, Instant createdTo,
                                                           Pageable pageable) {
        return stockReservationRepository.findAll(
                        stockReservationSpecification(
                                status,
                                inventoryItemId,
                                productVariantId,
                                normalizeSearch(sku),
                                referenceType,
                                normalizeExact(referenceId),
                                createdFrom,
                                createdTo
                        ),
                        pageable
                )
                .map(stockReservationMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public InventorySummaryResponse summary() {
        long itemCount = inventoryItemRepository.count();
        long activeItemCount = inventoryItemRepository.countByActiveTrue();
        long inactiveItemCount = inventoryItemRepository.countByActiveFalse();
        long lowStockItemCount = inventoryItemRepository.countLowStockItems();
        long totalQuantityOnHand = inventoryItemRepository.sumQuantityOnHand();
        long totalReservedQuantity = inventoryItemRepository.sumReservedQuantity();
        return new InventorySummaryResponse(
                itemCount,
                activeItemCount,
                inactiveItemCount,
                lowStockItemCount,
                totalQuantityOnHand,
                totalReservedQuantity,
                totalQuantityOnHand - totalReservedQuantity
        );
    }

    private InventoryItemResponse adjustQuantityOnHand(AdjustStockRequest request) {
        InventoryItem item = resolveItem(request.inventoryItemId(), request.sku(), request.productVariantId());
        int beforeQuantityOnHand = item.getQuantityOnHand();
        int beforeReservedQuantity = item.getReservedQuantity();
        int targetQuantity = request.quantity();

        item.adjustQuantityOnHand(targetQuantity);
        recordMovement(item, StockMovementType.ADJUSTMENT, Math.abs(targetQuantity - beforeQuantityOnHand), request.note(),
                defaultReferenceType(request.referenceType(), ReferenceType.ADMIN), request.referenceId(),
                beforeQuantityOnHand, item.getQuantityOnHand(), beforeReservedQuantity, item.getReservedQuantity());
        log.info("Adjusted inventory item id={} sku={} targetQuantity={}",
                item.getId(), item.getSku(), targetQuantity);
        return inventoryItemMapper.toResponse(item);
    }

    private void recordInitialMovement(InventoryItem item) {
        if (item.getQuantityOnHand() > 0) {
            recordMovement(item, StockMovementType.ADJUSTMENT, item.getQuantityOnHand(), "Initial stock",
                    ReferenceType.ADMIN, null, 0, item.getQuantityOnHand(), 0, item.getReservedQuantity());
        }
    }

    private void recordMovement(InventoryItem item, StockMovementType movementType, int quantity, String note,
                                ReferenceType referenceType, String referenceId, int beforeQuantityOnHand,
                                int afterQuantityOnHand, int beforeReservedQuantity, int afterReservedQuantity) {
        stockMovementRepository.save(new StockMovement(
                item,
                movementType,
                quantity,
                note,
                referenceType,
                referenceId,
                beforeQuantityOnHand,
                afterQuantityOnHand,
                beforeReservedQuantity,
                afterReservedQuantity,
                note,
                operatorId(referenceType, referenceId)
        ));
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

    private StockReservation getReservationEntity(UUID id) {
        return stockReservationRepository.findWithInventoryItemById(id)
                .orElseThrow(() -> new ReservationNotFoundException("Stock reservation not found: " + id));
    }

    private StockReservation getReservationForOperation(UUID id, String reservationKey) {
        if (id != null) {
            return getReservationEntity(id);
        }
        if (reservationKey != null && !reservationKey.isBlank()) {
            return stockReservationRepository.findByReservationKey(reservationKey.trim())
                    .orElseThrow(() -> new ReservationNotFoundException(
                            "Stock reservation not found for key: " + reservationKey
                    ));
        }
        throw new IllegalArgumentException("reservationId or reservationKey must be provided");
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
            if (!normalizeSku(variant.sku()).equals(sku)) {
                throw new ExternalCatalogValidationException("Inventory SKU must match catalog variant SKU");
            }
            if (!ACTIVE_PRODUCT_STATUS.equals(variant.productStatus())) {
                throw new ExternalCatalogValidationException("Inventory cannot be created for inactive product");
            }
            if (!variant.variantActive()) {
                throw new ExternalCatalogValidationException("Inventory cannot be created for inactive product variant");
            }
        } catch (FeignException.NotFound ex) {
            throw new ExternalCatalogValidationException("Catalog product variant not found: " + productVariantId);
        } catch (FeignException ex) {
            throw new ExternalServiceUnavailableException("Catalog service is unavailable");
        } catch (RuntimeException ex) {
            if (ex instanceof ExternalCatalogValidationException) {
                throw ex;
            }
            throw new ExternalServiceUnavailableException("Catalog service is unavailable");
        }
    }

    private StockReservationResponse handleDuplicateReserve(StockReservation existingReservation, InventoryItem item,
                                                           int requestedQuantity) {
        if (existingReservation.matches(item, requestedQuantity)) {
            log.info("Returned existing reservation reservationId={} key={}",
                    existingReservation.getId(), existingReservation.getReservationKey());
            return stockReservationMapper.toResponse(existingReservation);
        }
        if (!existingReservation.isActive()) {
            throw new InvalidReservationStateException("Reservation key was already used by a closed reservation");
        }
        throw new InvalidReservationStateException("Reservation key already exists with different stock details");
    }

    private AvailabilityResponse toAvailability(InventoryItem item, int requestedQuantity) {
        return new AvailabilityResponse(
                item.getId(),
                item.getProductVariantId(),
                item.getSku(),
                item.getQuantityOnHand(),
                item.getReservedQuantity(),
                item.availableQuantity(),
                requestedQuantity,
                item.isActive(),
                item.isLowStock(),
                item.isActive() && item.availableQuantity() >= requestedQuantity
        );
    }

    private Specification<StockMovement> stockMovementSpecification(UUID inventoryItemId, String sku,
                                                                    StockMovementType movementType,
                                                                    ReferenceType referenceType, String referenceId,
                                                                    Instant createdFrom, Instant createdTo) {
        Specification<StockMovement> specification = Specification.where(null);
        if (inventoryItemId != null) {
            specification = specification.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.equal(root.get("inventoryItem").get("id"), inventoryItemId));
        }
        if (!sku.isBlank()) {
            specification = specification.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("sku")), "%" + sku + "%"));
        }
        if (movementType != null) {
            specification = specification.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.equal(root.get("movementType"), movementType));
        }
        if (referenceType != null) {
            specification = specification.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.equal(root.get("referenceType"), referenceType));
        }
        if (!referenceId.isBlank()) {
            specification = specification.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.equal(root.get("referenceId"), referenceId));
        }
        if (createdFrom != null) {
            specification = specification.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.greaterThanOrEqualTo(root.get("createdAt"), createdFrom));
        }
        if (createdTo != null) {
            specification = specification.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.lessThanOrEqualTo(root.get("createdAt"), createdTo));
        }
        return specification;
    }

    private Specification<StockReservation> stockReservationSpecification(StockReservationStatus status,
                                                                         UUID inventoryItemId,
                                                                         UUID productVariantId,
                                                                         String sku,
                                                                         ReferenceType referenceType,
                                                                         String referenceId,
                                                                         Instant createdFrom,
                                                                         Instant createdTo) {
        Specification<StockReservation> specification = Specification.where(null);
        if (status != null) {
            specification = specification.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.equal(root.get("status"), status));
        }
        if (inventoryItemId != null) {
            specification = specification.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.equal(root.get("inventoryItem").get("id"), inventoryItemId));
        }
        if (productVariantId != null) {
            specification = specification.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.equal(root.get("productVariantId"), productVariantId));
        }
        if (!sku.isBlank()) {
            specification = specification.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("sku")), "%" + sku + "%"));
        }
        if (referenceType != null) {
            specification = specification.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.equal(root.get("referenceType"), referenceType));
        }
        if (!referenceId.isBlank()) {
            specification = specification.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.equal(root.get("referenceId"), referenceId));
        }
        if (createdFrom != null) {
            specification = specification.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.greaterThanOrEqualTo(root.get("createdAt"), createdFrom));
        }
        if (createdTo != null) {
            specification = specification.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.lessThanOrEqualTo(root.get("createdAt"), createdTo));
        }
        return specification;
    }

    private int operationQuantity(Integer requestedQuantity, StockReservation reservation) {
        return requestedQuantity == null ? reservation.getQuantity() : requestedQuantity;
    }

    private ReferenceType defaultReferenceType(ReferenceType referenceType, ReferenceType fallback) {
        return referenceType == null ? fallback : referenceType;
    }

    private String reservationKey(String reservationKey, ReferenceType referenceType, String referenceId,
                                  UUID productVariantId) {
        if (reservationKey != null && !reservationKey.isBlank()) {
            return reservationKey.trim();
        }
        return referenceType + ":" + referenceId + ":" + productVariantId;
    }

    private String requiredReferenceId(String referenceId) {
        if (referenceId == null || referenceId.isBlank()) {
            throw new InvalidReservationStateException("referenceId must be provided for stock reservations");
        }
        return referenceId.trim();
    }

    private void ensureNoInitialReservation(int reservedQuantity) {
        if (reservedQuantity > 0) {
            throw new InvalidStockStateException("Use reservation endpoints to reserve stock");
        }
    }

    private void ensureReservedQuantityUnchanged(InventoryItem item, int requestedReservedQuantity) {
        if (requestedReservedQuantity != item.getReservedQuantity()) {
            throw new InvalidStockStateException("Use reservation endpoints to change reserved quantity");
        }
    }

    private int lowStockThreshold(Integer threshold) {
        int resolvedThreshold = threshold == null ? DEFAULT_LOW_STOCK_THRESHOLD : threshold;
        validateThreshold(resolvedThreshold);
        return resolvedThreshold;
    }

    private void validateThreshold(Integer threshold) {
        if (threshold != null && threshold < 0) {
            throw new InvalidStockStateException("Low stock threshold cannot be negative");
        }
    }

    private String operatorId(ReferenceType referenceType, String referenceId) {
        return referenceType == ReferenceType.ADMIN ? referenceId : null;
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

    private String normalizeExact(String value) {
        return value == null || value.isBlank() ? "" : value.trim();
    }
}
