package com.ascmoda.catalog.domain.repository;

import com.ascmoda.catalog.domain.model.ProductImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ProductImageRepository extends JpaRepository<ProductImage, UUID> {

    List<ProductImage> findByProductIdAndActiveTrueOrderBySortOrderAsc(UUID productId);
}
