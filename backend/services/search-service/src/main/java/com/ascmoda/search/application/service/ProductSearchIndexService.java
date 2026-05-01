package com.ascmoda.search.application.service;

import com.ascmoda.search.application.dto.ProductSearchResponse;
import com.ascmoda.search.application.dto.SearchPageResponse;
import com.ascmoda.search.domain.exception.InvalidSearchRequestException;
import com.ascmoda.search.domain.exception.SearchDocumentNotFoundException;
import com.ascmoda.search.domain.exception.SearchIndexingException;
import com.ascmoda.search.infrastructure.elasticsearch.ProductSearchDocument;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.Criteria;
import org.springframework.data.elasticsearch.core.query.CriteriaQuery;
import org.springframework.data.elasticsearch.core.query.DeleteQuery;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
public class ProductSearchIndexService {

    private static final String ACTIVE_STATUS = "ACTIVE";
    private static final int MAX_PAGE_SIZE = 100;

    private final ElasticsearchOperations elasticsearchOperations;
    private final IndexCoordinates productIndex;

    public ProductSearchIndexService(ElasticsearchOperations elasticsearchOperations,
                                     @Value("${ascmoda.search.index.products:ascmoda-products}") String productIndex) {
        this.elasticsearchOperations = elasticsearchOperations;
        this.productIndex = IndexCoordinates.of(productIndex);
    }

    public void upsert(ProductSearchDocument document) {
        try {
            elasticsearchOperations.save(document, productIndex);
        } catch (RuntimeException ex) {
            throw new SearchIndexingException("Elasticsearch product upsert failed", ex);
        }
    }

    public void deleteByProductId(UUID productId) {
        try {
            Query query = new CriteriaQuery(new Criteria("productId").is(productId));
            elasticsearchOperations.delete(DeleteQuery.builder(query).build(), ProductSearchDocument.class, productIndex);
        } catch (RuntimeException ex) {
            throw new SearchIndexingException("Elasticsearch product delete failed", ex);
        }
    }

    public ProductSearchResponse getBySlug(String slug) {
        Criteria criteria = activeCriteria().and(new Criteria("productSlug").is(slug));
        Query query = new CriteriaQuery(criteria, PageRequest.of(0, 1));
        SearchHits<ProductSearchDocument> hits = search(query);
        return hits.stream()
                .findFirst()
                .map(SearchHit::getContent)
                .map(this::toResponse)
                .orElseThrow(() -> new SearchDocumentNotFoundException("Product search document not found: " + slug));
    }

    public SearchPageResponse<ProductSearchResponse> search(String q, String categorySlug, BigDecimal minPrice,
                                                            BigDecimal maxPrice, String sort, int page, int size) {
        PageRequest pageable = PageRequest.of(normalizePage(page), normalizeSize(size), sort(sort));
        Criteria criteria = activeCriteria();
        if (q != null && !q.isBlank()) {
            criteria = criteria.and(new Criteria("searchableText").contains(q.trim()));
        }
        if (categorySlug != null && !categorySlug.isBlank()) {
            criteria = criteria.and(new Criteria("categorySlug").is(categorySlug.trim()));
        }
        if (minPrice != null) {
            criteria = criteria.and(new Criteria("minPrice").greaterThanEqual(minPrice));
        }
        if (maxPrice != null) {
            criteria = criteria.and(new Criteria("minPrice").lessThanEqual(maxPrice));
        }

        SearchHits<ProductSearchDocument> hits = search(new CriteriaQuery(criteria, pageable));
        List<ProductSearchResponse> content = hits.stream()
                .map(SearchHit::getContent)
                .map(this::toResponse)
                .toList();
        long total = hits.getTotalHits();
        int totalPages = total == 0 ? 0 : (int) Math.ceil((double) total / pageable.getPageSize());
        return new SearchPageResponse<>(
                content,
                pageable.getPageNumber(),
                pageable.getPageSize(),
                total,
                totalPages,
                pageable.getPageNumber() == 0,
                totalPages == 0 || pageable.getPageNumber() + 1 >= totalPages
        );
    }

    private SearchHits<ProductSearchDocument> search(Query query) {
        try {
            return elasticsearchOperations.search(query, ProductSearchDocument.class, productIndex);
        } catch (RuntimeException ex) {
            throw new SearchIndexingException("Elasticsearch product search failed", ex);
        }
    }

    private Criteria activeCriteria() {
        return new Criteria("status").is(ACTIVE_STATUS);
    }

    private int normalizePage(int page) {
        if (page < 0) {
            throw new InvalidSearchRequestException("Page must be zero or greater");
        }
        return page;
    }

    private int normalizeSize(int size) {
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new InvalidSearchRequestException("Size must be between 1 and " + MAX_PAGE_SIZE);
        }
        return size;
    }

    private Sort sort(String value) {
        String normalized = value == null || value.isBlank() ? "relevance" : value.trim();
        return switch (normalized) {
            case "relevance" -> Sort.unsorted();
            case "priceAsc" -> Sort.by(Sort.Direction.ASC, "minPrice");
            case "priceDesc" -> Sort.by(Sort.Direction.DESC, "minPrice");
            case "newest", "updatedAt" -> Sort.by(Sort.Direction.DESC, "updatedAt");
            default -> throw new InvalidSearchRequestException("Unsupported search sort: " + value);
        };
    }

    private ProductSearchResponse toResponse(ProductSearchDocument document) {
        return new ProductSearchResponse(
                document.getProductId(),
                document.getProductName(),
                document.getProductSlug(),
                document.getShortDescription(),
                document.getCategoryName(),
                document.getCategorySlug(),
                document.getMainImageUrl(),
                document.getMinPrice(),
                document.getMaxPrice(),
                document.getVariantCount(),
                document.isAvailable(),
                document.getUpdatedAt()
        );
    }
}
