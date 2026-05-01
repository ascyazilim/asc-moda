package com.ascmoda.search.application.service;

import com.ascmoda.search.infrastructure.elasticsearch.ProductSearchDocument;
import com.ascmoda.shared.kernel.event.catalog.CatalogProductCreatedEvent;
import com.ascmoda.shared.kernel.event.catalog.CatalogProductUpdatedEvent;
import com.ascmoda.shared.kernel.event.catalog.CatalogProductVariantEvent;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class ProductSearchDocumentFactory {

    public ProductSearchDocument from(CatalogProductCreatedEvent event) {
        return new ProductSearchDocument(
                event.productId(),
                event.productName(),
                event.productSlug(),
                event.shortDescription(),
                event.description(),
                event.categoryId(),
                event.categoryName(),
                event.categorySlug(),
                event.status(),
                event.mainImageUrl(),
                event.minPrice(),
                event.maxPrice(),
                activeVariantCount(event.variants()),
                hasActiveVariant(event.variants()),
                event.searchableText(),
                updatedAt(event.occurredAt())
        );
    }

    public ProductSearchDocument from(CatalogProductUpdatedEvent event) {
        return new ProductSearchDocument(
                event.productId(),
                event.productName(),
                event.productSlug(),
                event.shortDescription(),
                event.description(),
                event.categoryId(),
                event.categoryName(),
                event.categorySlug(),
                event.status(),
                event.mainImageUrl(),
                event.minPrice(),
                event.maxPrice(),
                activeVariantCount(event.variants()),
                hasActiveVariant(event.variants()),
                event.searchableText(),
                updatedAt(event.occurredAt())
        );
    }

    private int activeVariantCount(java.util.List<CatalogProductVariantEvent> variants) {
        if (variants == null) {
            return 0;
        }
        return (int) variants.stream().filter(CatalogProductVariantEvent::active).count();
    }

    private boolean hasActiveVariant(java.util.List<CatalogProductVariantEvent> variants) {
        return variants != null && variants.stream().anyMatch(CatalogProductVariantEvent::active);
    }

    private Instant updatedAt(Instant occurredAt) {
        return occurredAt == null ? Instant.now() : occurredAt;
    }
}
