package com.ascmoda.catalog.application.service;

import com.ascmoda.catalog.api.dto.ProductVariantResponse;
import com.ascmoda.catalog.api.dto.ProductVariantDetailResponse;
import com.ascmoda.catalog.api.error.ResourceNotFoundException;
import com.ascmoda.catalog.application.mapper.ProductVariantMapper;
import com.ascmoda.catalog.domain.model.Product;
import com.ascmoda.catalog.domain.model.ProductImage;
import com.ascmoda.catalog.domain.model.ProductVariant;
import com.ascmoda.catalog.domain.repository.ProductVariantRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class ProductVariantService {

    private final ProductVariantRepository productVariantRepository;
    private final ProductVariantMapper productVariantMapper;

    public ProductVariantService(ProductVariantRepository productVariantRepository,
                                 ProductVariantMapper productVariantMapper) {
        this.productVariantRepository = productVariantRepository;
        this.productVariantMapper = productVariantMapper;
    }

    @Transactional(readOnly = true)
    public ProductVariantResponse getById(UUID id) {
        return productVariantRepository.findById(id)
                .map(productVariantMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Product variant not found: " + id));
    }

    @Transactional(readOnly = true)
    public ProductVariantDetailResponse getDetailsById(UUID id) {
        ProductVariant variant = productVariantRepository.findWithProductById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product variant not found: " + id));
        Product product = variant.getProduct();

        return new ProductVariantDetailResponse(
                variant.getId(),
                product.getId(),
                product.getName(),
                product.getSlug(),
                product.getStatus(),
                variant.getSku(),
                variant.getColor(),
                variant.getSize(),
                variant.getPriceOverride() == null ? product.getBasePrice() : variant.getPriceOverride(),
                resolveMainImageUrl(product),
                variant.isActive()
        );
    }

    private String resolveMainImageUrl(Product product) {
        return product.getImages()
                .stream()
                .filter(image -> image.isActive() && image.isMain())
                .findFirst()
                .map(ProductImage::getImageUrl)
                .orElse(null);
    }
}
