package com.ascmoda.catalog.application.service;

import com.ascmoda.catalog.api.dto.CreateProductRequest;
import com.ascmoda.catalog.api.dto.CreateProductVariantRequest;
import com.ascmoda.catalog.api.dto.ProductImageRequest;
import com.ascmoda.catalog.api.dto.ProductImageResponse;
import com.ascmoda.catalog.api.dto.ProductResponse;
import com.ascmoda.catalog.api.dto.ProductVariantResponse;
import com.ascmoda.catalog.api.dto.UpdateProductRequest;
import com.ascmoda.catalog.api.error.BusinessRuleViolationException;
import com.ascmoda.catalog.api.error.DuplicateResourceException;
import com.ascmoda.catalog.api.error.ResourceNotFoundException;
import com.ascmoda.catalog.application.mapper.ProductImageMapper;
import com.ascmoda.catalog.application.mapper.ProductVariantMapper;
import com.ascmoda.catalog.domain.model.Category;
import com.ascmoda.catalog.domain.model.Product;
import com.ascmoda.catalog.domain.model.ProductImage;
import com.ascmoda.catalog.domain.model.ProductStatus;
import com.ascmoda.catalog.domain.model.ProductVariant;
import com.ascmoda.catalog.domain.repository.CategoryRepository;
import com.ascmoda.catalog.domain.repository.ProductRepository;
import com.ascmoda.catalog.domain.repository.ProductVariantRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;

@Service
public class ProductService {

    private static final Logger log = LoggerFactory.getLogger(ProductService.class);
    private static final Set<String> SUPPORTED_SORTS = Set.of("createdAt", "name", "basePrice");
    private static final Sort DEFAULT_SORT = Sort.by(Sort.Direction.DESC, "createdAt");
    private static final int MAX_PAGE_SIZE = 100;

    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;
    private final CategoryRepository categoryRepository;
    private final ProductVariantMapper productVariantMapper;
    private final ProductImageMapper productImageMapper;
    private final SlugGenerator slugGenerator;
    private final CatalogOutboxService catalogOutboxService;

    public ProductService(ProductRepository productRepository, ProductVariantRepository productVariantRepository,
                          CategoryRepository categoryRepository, ProductVariantMapper productVariantMapper,
                          ProductImageMapper productImageMapper, SlugGenerator slugGenerator,
                          CatalogOutboxService catalogOutboxService) {
        this.productRepository = productRepository;
        this.productVariantRepository = productVariantRepository;
        this.categoryRepository = categoryRepository;
        this.productVariantMapper = productVariantMapper;
        this.productImageMapper = productImageMapper;
        this.slugGenerator = slugGenerator;
        this.catalogOutboxService = catalogOutboxService;
    }

    @Transactional
    public ProductResponse create(CreateProductRequest request) {
        Category category = getCategory(request.categoryId());
        String slug = resolveProductSlug(request.slug(), request.name(), null);
        ensureVariantSkusAvailable(null, request.variants());

        Product product = new Product(
                request.name().trim(),
                slug,
                request.description(),
                request.shortDescription(),
                request.basePrice(),
                request.status() == null ? ProductStatus.DRAFT : request.status(),
                category
        );

        syncVariants(product, request.variants());
        syncImages(product, request.images());
        validateImages(product);

        Product saved = productRepository.saveAndFlush(product);
        catalogOutboxService.recordProductCreated(saved);
        log.info("Created catalog product id={} slug={}", saved.getId(), saved.getSlug());
        return toAdminResponse(saved);
    }

    @Transactional
    public ProductResponse update(UUID id, UpdateProductRequest request) {
        Product product = getProduct(id);
        Category category = getCategory(request.categoryId());
        String slug = resolveProductSlug(request.slug(), request.name(), id);

        product.setName(request.name().trim());
        product.setSlug(slug);
        product.setDescription(request.description());
        product.setShortDescription(request.shortDescription());
        product.setBasePrice(request.basePrice());
        product.setStatus(request.status() == null ? ProductStatus.DRAFT : request.status());
        product.setCategory(category);

        if (request.variants() != null) {
            ensureVariantSkusAvailable(product, request.variants());
            syncVariants(product, request.variants());
        }

        if (request.images() != null) {
            syncImages(product, request.images());
            validateImages(product);
        }

        Product saved = productRepository.saveAndFlush(product);
        if (saved.getStatus() == ProductStatus.INACTIVE) {
            catalogOutboxService.recordProductDeactivated(saved);
        } else {
            catalogOutboxService.recordProductUpdated(saved);
        }
        log.info("Updated catalog product id={} slug={}", saved.getId(), saved.getSlug());
        return toAdminResponse(saved);
    }

    @Transactional
    public ProductResponse changeStatus(UUID id, ProductStatus status) {
        Product product = getProduct(id);
        if (status == null) {
            throw new BusinessRuleViolationException("Product status must be provided");
        }
        product.setStatus(status);

        Product saved = productRepository.saveAndFlush(product);
        if (saved.getStatus() == ProductStatus.INACTIVE) {
            catalogOutboxService.recordProductDeactivated(saved);
        } else {
            catalogOutboxService.recordProductUpdated(saved);
        }
        log.info("Changed catalog product id={} status={}", saved.getId(), saved.getStatus());
        return toAdminResponse(saved);
    }

    @Transactional
    public ProductResponse activate(UUID id) {
        return changeStatus(id, ProductStatus.ACTIVE);
    }

    @Transactional
    public ProductResponse deactivate(UUID id) {
        return changeStatus(id, ProductStatus.INACTIVE);
    }

    @Transactional(readOnly = true)
    public Page<ProductResponse> listPublic(String categorySlug, String q, Pageable pageable) {
        Pageable normalizedPageable = normalizePageable(pageable);
        return productRepository.searchPublic(
                        ProductStatus.ACTIVE,
                        normalizeFilter(categorySlug),
                        normalizeSearch(q),
                        normalizedPageable
                )
                .map(this::toPublicResponse);
    }

    @Transactional(readOnly = true)
    public Page<ProductResponse> listAdmin(ProductStatus status, Pageable pageable) {
        return productRepository.searchAdmin(status, normalizePageable(pageable))
                .map(this::toAdminResponse);
    }

    @Transactional(readOnly = true)
    public ProductResponse getPublicBySlug(String slug) {
        Product product = productRepository.findPublicBySlug(slug, ProductStatus.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException("Active product not found for slug: " + slug));
        return toPublicResponse(product);
    }

    @Transactional(readOnly = true)
    public ProductResponse getAdminById(UUID id) {
        return toAdminResponse(getProduct(id));
    }

    private Product getProduct(UUID id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + id));
    }

    private Category getCategory(UUID id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found: " + id));
    }

    private ProductResponse toPublicResponse(Product product) {
        return toResponse(product, true);
    }

    private ProductResponse toAdminResponse(Product product) {
        return toResponse(product, false);
    }

    private ProductResponse toResponse(Product product, boolean publicView) {
        List<ProductVariantResponse> variants = product.getVariants()
                .stream()
                .filter(variant -> !publicView || variant.isActive())
                .sorted(Comparator.comparing(ProductVariant::getSku))
                .map(productVariantMapper::toResponse)
                .toList();

        List<ProductImageResponse> images = product.getImages()
                .stream()
                .filter(image -> !publicView || isPublicImageVisible(image))
                .sorted(Comparator.comparingInt(ProductImage::getSortOrder)
                        .thenComparing(ProductImage::getImageUrl))
                .map(productImageMapper::toResponse)
                .toList();

        return new ProductResponse(
                product.getId(),
                product.getName(),
                product.getSlug(),
                product.getDescription(),
                product.getShortDescription(),
                product.getBasePrice(),
                product.getStatus(),
                product.getCategory().getId(),
                product.getCategory().getName(),
                product.getCategory().getSlug(),
                variants,
                images,
                product.getVersion(),
                product.getCreatedAt(),
                product.getUpdatedAt()
        );
    }

    private boolean isPublicImageVisible(ProductImage image) {
        ProductVariant variant = image.getVariant();
        return image.isActive() && (variant == null || variant.isActive());
    }

    private String resolveProductSlug(String requestedSlug, String name, UUID currentProductId) {
        String baseSlug = slugGenerator.generate(resolveSlugSource(requestedSlug, name));
        Predicate<String> exists = currentProductId == null
                ? productRepository::existsBySlug
                : slug -> productRepository.existsBySlugAndIdNot(slug, currentProductId);

        if (requestedSlug != null && !requestedSlug.isBlank()) {
            ensureProductSlugAvailable(baseSlug, exists);
            return baseSlug;
        }

        return slugGenerator.generateUnique(baseSlug, exists);
    }

    private void ensureProductSlugAvailable(String slug, Predicate<String> exists) {
        if (exists.test(slug)) {
            throw new DuplicateResourceException("Product slug already exists: " + slug);
        }
    }

    private void ensureVariantSkusAvailable(Product product, List<CreateProductVariantRequest> variants) {
        if (variants == null || variants.isEmpty()) {
            return;
        }

        Map<UUID, ProductVariant> existingVariants = product == null ? Map.of() : variantsById(product);
        Set<String> requestSkus = new HashSet<>();
        Set<UUID> requestIds = new HashSet<>();

        for (CreateProductVariantRequest variant : variants) {
            UUID variantId = variant.id();
            String sku = variant.sku().trim();

            if (variantId != null && !requestIds.add(variantId)) {
                throw new DuplicateResourceException("Duplicate product variant id in request: " + variantId);
            }
            if (product == null && variantId != null) {
                throw new BusinessRuleViolationException("Variant id is not allowed when creating a product");
            }
            if (product != null && variantId != null && !existingVariants.containsKey(variantId)) {
                throw new BusinessRuleViolationException("Product variant does not belong to product: " + variantId);
            }
            if (!requestSkus.add(sku)) {
                throw new DuplicateResourceException("Duplicate product variant SKU in request: " + sku);
            }
            if (variantId == null && productVariantRepository.existsBySku(sku)) {
                throw new DuplicateResourceException("Product variant SKU already exists: " + sku);
            }
            if (variantId != null && productVariantRepository.existsBySkuAndIdNot(sku, variantId)) {
                throw new DuplicateResourceException("Product variant SKU already exists: " + sku);
            }
        }
    }

    private void syncVariants(Product product, List<CreateProductVariantRequest> variants) {
        if (variants == null) {
            return;
        }

        Map<UUID, ProductVariant> existingVariants = variantsById(product);
        Set<UUID> incomingIds = new HashSet<>();

        for (CreateProductVariantRequest request : variants) {
            if (request.id() == null) {
                ProductVariant variant = productVariantMapper.toEntity(request);
                variant.setSku(request.sku().trim());
                product.addVariant(variant);
                continue;
            }

            ProductVariant variant = existingVariants.get(request.id());
            incomingIds.add(request.id());
            updateVariant(variant, request);
        }

        for (ProductVariant variant : product.getVariants()) {
            UUID variantId = variant.getId();
            if (variantId != null && !incomingIds.contains(variantId)) {
                variant.setActive(false);
            }
        }
    }

    private void updateVariant(ProductVariant variant, CreateProductVariantRequest request) {
        variant.setSku(request.sku().trim());
        variant.setColor(request.color());
        variant.setSize(request.size());
        variant.setStockKeepingNote(request.stockKeepingNote());
        variant.setPriceOverride(request.priceOverride());
        variant.setActive(request.active() == null || request.active());
    }

    private void syncImages(Product product, List<ProductImageRequest> images) {
        if (images == null) {
            return;
        }

        Map<UUID, ProductImage> existingImages = imagesById(product);
        Set<UUID> incomingIds = new HashSet<>();
        int fallbackSortOrder = 0;

        for (ProductImageRequest request : images) {
            if (request.id() != null && !incomingIds.add(request.id())) {
                throw new DuplicateResourceException("Duplicate product image id in request: " + request.id());
            }

            ProductVariant variant = resolveVariantForImage(product, request.variantId());
            int sortOrder = request.sortOrder() == null ? fallbackSortOrder : request.sortOrder();
            fallbackSortOrder++;

            if (request.id() == null) {
                ProductImage image = new ProductImage(
                        variant,
                        request.imageUrl().trim(),
                        request.altText(),
                        sortOrder,
                        request.main() != null && request.main(),
                        request.active() == null || request.active()
                );
                product.addImage(image);
                continue;
            }

            ProductImage image = existingImages.get(request.id());
            if (image == null) {
                throw new BusinessRuleViolationException("Product image does not belong to product: " + request.id());
            }
            image.setVariant(variant);
            image.setImageUrl(request.imageUrl().trim());
            image.setAltText(request.altText());
            image.setSortOrder(sortOrder);
            image.setMain(request.main() != null && request.main());
            image.setActive(request.active() == null || request.active());
        }

        for (ProductImage image : product.getImages()) {
            UUID imageId = image.getId();
            if (imageId != null && !incomingIds.contains(imageId)) {
                image.setActive(false);
            }
        }
    }

    private ProductVariant resolveVariantForImage(Product product, UUID variantId) {
        if (variantId == null) {
            return null;
        }

        return product.getVariants()
                .stream()
                .filter(variant -> variantId.equals(variant.getId()))
                .findFirst()
                .orElseThrow(() -> new BusinessRuleViolationException(
                        "Product image variant does not belong to product: " + variantId
                ));
    }

    private void validateImages(Product product) {
        long mainImageCount = product.getImages()
                .stream()
                .filter(image -> image.isActive() && image.isMain())
                .count();

        if (mainImageCount > 1) {
            throw new BusinessRuleViolationException("A product can have only one active main image");
        }

        for (ProductImage image : product.getImages()) {
            ProductVariant variant = image.getVariant();
            if (variant != null && !variantBelongsToProduct(product, variant)) {
                throw new BusinessRuleViolationException("Product image variant must belong to the same product");
            }
        }
    }

    private boolean variantBelongsToProduct(Product product, ProductVariant candidate) {
        return product.getVariants()
                .stream()
                .anyMatch(variant -> sameVariant(variant, candidate));
    }

    private boolean sameVariant(ProductVariant left, ProductVariant right) {
        if (left.getId() == null || right.getId() == null) {
            return left == right;
        }
        return left.getId().equals(right.getId());
    }

    private Map<UUID, ProductVariant> variantsById(Product product) {
        Map<UUID, ProductVariant> variants = new HashMap<>();
        for (ProductVariant variant : product.getVariants()) {
            if (variant.getId() != null) {
                variants.put(variant.getId(), variant);
            }
        }
        return variants;
    }

    private Map<UUID, ProductImage> imagesById(Product product) {
        Map<UUID, ProductImage> images = new HashMap<>();
        for (ProductImage image : product.getImages()) {
            if (image.getId() != null) {
                images.put(image.getId(), image);
            }
        }
        return images;
    }

    private Pageable normalizePageable(Pageable pageable) {
        Sort requestedSort = pageable.getSort().isSorted() ? pageable.getSort() : DEFAULT_SORT;
        requestedSort.forEach(order -> {
            if (!SUPPORTED_SORTS.contains(order.getProperty())) {
                throw new BusinessRuleViolationException("Unsupported product sort property: " + order.getProperty());
            }
        });

        return PageRequest.of(pageable.getPageNumber(), Math.min(pageable.getPageSize(), MAX_PAGE_SIZE), requestedSort);
    }

    private String normalizeFilter(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String normalizeSearch(String value) {
        return value == null || value.isBlank() ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private String resolveSlugSource(String slug, String name) {
        return slug == null || slug.isBlank() ? name : slug;
    }
}
