package com.ascmoda.catalog.application.service;

import com.ascmoda.catalog.domain.model.Product;
import com.ascmoda.catalog.domain.model.ProductImage;
import com.ascmoda.catalog.domain.model.ProductVariant;
import com.ascmoda.shared.kernel.event.EventEnvelope;
import com.ascmoda.shared.kernel.event.EventTypes;
import com.ascmoda.shared.kernel.event.catalog.CatalogProductCreatedEvent;
import com.ascmoda.shared.kernel.event.catalog.CatalogProductDeactivatedEvent;
import com.ascmoda.shared.kernel.event.catalog.CatalogProductUpdatedEvent;
import com.ascmoda.shared.kernel.event.catalog.CatalogProductVariantEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Stream;

@Component
public class CatalogEventFactory {

    private static final String SOURCE_SERVICE = "catalog-service";

    private final ObjectMapper objectMapper;

    public CatalogEventFactory(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public EventDocument productCreated(Product product) {
        UUID eventId = UUID.randomUUID();
        Instant occurredAt = Instant.now();
        return toDocument(
                eventId,
                EventTypes.CATALOG_PRODUCT_CREATED,
                occurredAt,
                product.getId().toString(),
                createdPayload(product, occurredAt)
        );
    }

    public EventDocument productUpdated(Product product) {
        UUID eventId = UUID.randomUUID();
        Instant occurredAt = Instant.now();
        return toDocument(
                eventId,
                EventTypes.CATALOG_PRODUCT_UPDATED,
                occurredAt,
                product.getId().toString(),
                updatedPayload(product, occurredAt)
        );
    }

    public EventDocument productDeactivated(Product product) {
        UUID eventId = UUID.randomUUID();
        Instant occurredAt = Instant.now();
        CatalogProductDeactivatedEvent payload = new CatalogProductDeactivatedEvent(
                product.getId(),
                product.getSlug(),
                product.getStatus().name(),
                occurredAt,
                SOURCE_SERVICE
        );
        return toDocument(
                eventId,
                EventTypes.CATALOG_PRODUCT_DEACTIVATED,
                occurredAt,
                product.getId().toString(),
                payload
        );
    }

    private CatalogProductCreatedEvent createdPayload(Product product, Instant occurredAt) {
        PriceRange priceRange = priceRange(product);
        return new CatalogProductCreatedEvent(
                product.getId(),
                product.getName(),
                product.getSlug(),
                product.getShortDescription(),
                product.getDescription(),
                product.getCategory().getId(),
                product.getCategory().getName(),
                product.getCategory().getSlug(),
                product.getStatus().name(),
                priceRange.minPrice(),
                priceRange.maxPrice(),
                variants(product),
                mainImageUrl(product),
                searchableText(product),
                occurredAt,
                SOURCE_SERVICE
        );
    }

    private CatalogProductUpdatedEvent updatedPayload(Product product, Instant occurredAt) {
        PriceRange priceRange = priceRange(product);
        return new CatalogProductUpdatedEvent(
                product.getId(),
                product.getName(),
                product.getSlug(),
                product.getShortDescription(),
                product.getDescription(),
                product.getCategory().getId(),
                product.getCategory().getName(),
                product.getCategory().getSlug(),
                product.getStatus().name(),
                priceRange.minPrice(),
                priceRange.maxPrice(),
                variants(product),
                mainImageUrl(product),
                searchableText(product),
                occurredAt,
                SOURCE_SERVICE
        );
    }

    private List<CatalogProductVariantEvent> variants(Product product) {
        return product.getVariants()
                .stream()
                .sorted(Comparator.comparing(ProductVariant::getSku))
                .map(variant -> new CatalogProductVariantEvent(
                        variant.getId(),
                        variant.getSku(),
                        variant.getColor(),
                        variant.getSize(),
                        effectivePrice(product, variant),
                        variant.isActive()
                ))
                .toList();
    }

    private PriceRange priceRange(Product product) {
        List<BigDecimal> prices = product.getVariants()
                .stream()
                .filter(ProductVariant::isActive)
                .map(variant -> effectivePrice(product, variant))
                .toList();
        if (prices.isEmpty()) {
            return new PriceRange(product.getBasePrice(), product.getBasePrice());
        }
        BigDecimal min = prices.stream().min(Comparator.naturalOrder()).orElse(product.getBasePrice());
        BigDecimal max = prices.stream().max(Comparator.naturalOrder()).orElse(product.getBasePrice());
        return new PriceRange(min, max);
    }

    private BigDecimal effectivePrice(Product product, ProductVariant variant) {
        return variant.getPriceOverride() == null ? product.getBasePrice() : variant.getPriceOverride();
    }

    private String mainImageUrl(Product product) {
        return product.getImages()
                .stream()
                .filter(ProductImage::isActive)
                .sorted(Comparator.comparing(ProductImage::isMain).reversed()
                        .thenComparingInt(ProductImage::getSortOrder)
                        .thenComparing(ProductImage::getImageUrl))
                .map(ProductImage::getImageUrl)
                .findFirst()
                .orElse(null);
    }

    private String searchableText(Product product) {
        String value = Stream.concat(
                        Stream.of(
                                product.getName(),
                                product.getSlug(),
                                product.getShortDescription(),
                                product.getDescription(),
                                product.getCategory().getName(),
                                product.getCategory().getSlug()
                        ),
                        product.getVariants().stream()
                                .flatMap(variant -> Stream.of(variant.getSku(), variant.getColor(), variant.getSize()))
                )
                .filter(text -> text != null && !text.isBlank())
                .map(String::trim)
                .reduce("", (left, right) -> left + " " + right);
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private EventDocument toDocument(UUID eventId, String eventType, Instant occurredAt, String correlationId,
                                     Object payload) {
        EventEnvelope<Object> envelope = new EventEnvelope<>(
                eventId,
                eventType,
                occurredAt,
                SOURCE_SERVICE,
                correlationId,
                payload
        );
        try {
            return new EventDocument(eventId, eventType, occurredAt, correlationId, objectMapper.writeValueAsString(envelope));
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Catalog event payload could not be serialized", ex);
        }
    }

    private record PriceRange(BigDecimal minPrice, BigDecimal maxPrice) {
    }

    public record EventDocument(
            UUID eventId,
            String eventType,
            Instant occurredAt,
            String correlationId,
            String payloadJson
    ) {
    }
}
