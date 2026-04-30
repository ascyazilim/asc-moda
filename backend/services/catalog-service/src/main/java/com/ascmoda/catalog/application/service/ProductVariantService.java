package com.ascmoda.catalog.application.service;

import com.ascmoda.catalog.api.dto.ProductVariantResponse;
import com.ascmoda.catalog.api.error.ResourceNotFoundException;
import com.ascmoda.catalog.application.mapper.ProductVariantMapper;
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
}
