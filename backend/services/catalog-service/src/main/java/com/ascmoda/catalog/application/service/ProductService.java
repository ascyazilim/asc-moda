package com.ascmoda.catalog.application.service;

import com.ascmoda.catalog.api.dto.CreateProductRequest;
import com.ascmoda.catalog.api.dto.CreateProductVariantRequest;
import com.ascmoda.catalog.api.dto.ProductImageResponse;
import com.ascmoda.catalog.api.dto.ProductResponse;
import com.ascmoda.catalog.api.dto.ProductVariantResponse;
import com.ascmoda.catalog.api.dto.UpdateProductRequest;
import com.ascmoda.catalog.api.error.DuplicateResourceException;
import com.ascmoda.catalog.api.error.ResourceNotFoundException;
import com.ascmoda.catalog.application.mapper.ProductImageMapper;
import com.ascmoda.catalog.application.mapper.ProductMapper;
import com.ascmoda.catalog.application.mapper.ProductVariantMapper;
import com.ascmoda.catalog.domain.model.Category;
import com.ascmoda.catalog.domain.model.Product;
import com.ascmoda.catalog.domain.model.ProductStatus;
import com.ascmoda.catalog.domain.model.ProductVariant;
import com.ascmoda.catalog.domain.repository.CategoryRepository;
import com.ascmoda.catalog.domain.repository.ProductRepository;
import com.ascmoda.catalog.domain.repository.ProductVariantRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class ProductService {

    private static final Logger log = LoggerFactory.getLogger(ProductService.class);

    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;
    private final CategoryRepository categoryRepository;
    private final ProductMapper productMapper;
    private final ProductVariantMapper productVariantMapper;
    private final ProductImageMapper productImageMapper;
    private final SlugGenerator slugGenerator;

    public ProductService(ProductRepository productRepository, ProductVariantRepository productVariantRepository,
                          CategoryRepository categoryRepository, ProductMapper productMapper,
                          ProductVariantMapper productVariantMapper, ProductImageMapper productImageMapper,
                          SlugGenerator slugGenerator) {
        this.productRepository = productRepository;
        this.productVariantRepository = productVariantRepository;
        this.categoryRepository = categoryRepository;
        this.productMapper = productMapper;
        this.productVariantMapper = productVariantMapper;
        this.productImageMapper = productImageMapper;
        this.slugGenerator = slugGenerator;
    }

    @Transactional
    public ProductResponse create(CreateProductRequest request) {
        Category category = getCategory(request.categoryId());
        String slug = slugGenerator.generate(resolveSlugSource(request.slug(), request.name()));
        ensureProductSlugAvailable(slug);
        ensureVariantSkusAvailable(request.variants());

        Product product = new Product(
                request.name().trim(),
                slug,
                request.description(),
                request.shortDescription(),
                request.basePrice(),
                request.status() == null ? ProductStatus.DRAFT : request.status(),
                category
        );

        addVariants(product, request.variants());

        Product saved = productRepository.save(product);
        log.info("Created catalog product id={} slug={}", saved.getId(), saved.getSlug());
        return productMapper.toResponse(saved);
    }

    @Transactional
    public ProductResponse update(UUID id, UpdateProductRequest request) {
        Product product = getProduct(id);
        Category category = getCategory(request.categoryId());
        String slug = slugGenerator.generate(resolveSlugSource(request.slug(), request.name()));

        if (productRepository.existsBySlugAndIdNot(slug, id)) {
            throw new DuplicateResourceException("Product slug already exists: " + slug);
        }

        product.setName(request.name().trim());
        product.setSlug(slug);
        product.setDescription(request.description());
        product.setShortDescription(request.shortDescription());
        product.setBasePrice(request.basePrice());
        product.setStatus(request.status() == null ? ProductStatus.DRAFT : request.status());
        product.setCategory(category);

        if (request.variants() != null) {
            ensureVariantSkusAvailable(request.variants());
            product.deactivateVariants();
            addVariants(product, request.variants());
        }

        Product saved = productRepository.save(product);
        log.info("Updated catalog product id={} slug={}", saved.getId(), saved.getSlug());
        return productMapper.toResponse(saved);
    }

    @Transactional
    public ProductResponse changeStatus(UUID id, ProductStatus status) {
        Product product = getProduct(id);
        product.setStatus(status);

        Product saved = productRepository.save(product);
        log.info("Changed catalog product id={} status={}", saved.getId(), saved.getStatus());
        return productMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public Page<ProductResponse> listPublic(UUID categoryId, Pageable pageable) {
        Page<Product> products = categoryId == null
                ? productRepository.findByStatus(ProductStatus.ACTIVE, pageable)
                : productRepository.findByCategoryIdAndStatus(categoryId, ProductStatus.ACTIVE, pageable);

        return products.map(this::toPublicResponse);
    }

    @Transactional(readOnly = true)
    public Page<ProductResponse> listAdmin(Pageable pageable) {
        return productRepository.findAllByOrderByCreatedAtDesc(pageable)
                .map(productMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public ProductResponse getPublicBySlug(String slug) {
        Product product = productRepository.findBySlugAndStatus(slug, ProductStatus.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException("Active product not found for slug: " + slug));
        return toPublicResponse(product);
    }

    @Transactional(readOnly = true)
    public ProductResponse getAdminById(UUID id) {
        return productMapper.toResponse(getProduct(id));
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
        List<ProductVariantResponse> activeVariants = product.getVariants()
                .stream()
                .filter(ProductVariant::isActive)
                .map(productVariantMapper::toResponse)
                .toList();

        List<ProductImageResponse> activeImages = product.getImages()
                .stream()
                .filter(image -> image.isActive())
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
                activeVariants,
                activeImages,
                product.getVersion(),
                product.getCreatedAt(),
                product.getUpdatedAt()
        );
    }

    private void ensureProductSlugAvailable(String slug) {
        if (productRepository.existsBySlug(slug)) {
            throw new DuplicateResourceException("Product slug already exists: " + slug);
        }
    }

    private void ensureVariantSkusAvailable(List<CreateProductVariantRequest> variants) {
        if (variants == null || variants.isEmpty()) {
            return;
        }

        Set<String> requestSkus = new HashSet<>();
        for (CreateProductVariantRequest variant : variants) {
            String sku = variant.sku().trim();
            if (!requestSkus.add(sku)) {
                throw new DuplicateResourceException("Duplicate product variant SKU in request: " + sku);
            }
            if (productVariantRepository.existsBySku(sku)) {
                throw new DuplicateResourceException("Product variant SKU already exists: " + sku);
            }
        }
    }

    private void addVariants(Product product, List<CreateProductVariantRequest> variants) {
        if (variants == null || variants.isEmpty()) {
            return;
        }

        for (CreateProductVariantRequest request : variants) {
            ProductVariant variant = productVariantMapper.toEntity(request);
            variant.setSku(request.sku().trim());
            product.addVariant(variant);
        }
    }

    private String resolveSlugSource(String slug, String name) {
        return slug == null || slug.isBlank() ? name : slug;
    }
}
