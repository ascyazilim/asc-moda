package com.ascmoda.search.infrastructure.elasticsearch;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Document(indexName = "ascmoda-products")
public class ProductSearchDocument {

    @Id
    private String id;

    @Field(type = FieldType.Keyword)
    private UUID productId;

    @Field(type = FieldType.Text)
    private String productName;

    @Field(type = FieldType.Keyword)
    private String productSlug;

    @Field(type = FieldType.Text)
    private String shortDescription;

    @Field(type = FieldType.Text)
    private String description;

    @Field(type = FieldType.Keyword)
    private UUID categoryId;

    @Field(type = FieldType.Text)
    private String categoryName;

    @Field(type = FieldType.Keyword)
    private String categorySlug;

    @Field(type = FieldType.Keyword)
    private String status;

    @Field(type = FieldType.Keyword)
    private String mainImageUrl;

    @Field(type = FieldType.Double)
    private BigDecimal minPrice;

    @Field(type = FieldType.Double)
    private BigDecimal maxPrice;

    @Field(type = FieldType.Integer)
    private int variantCount;

    @Field(type = FieldType.Boolean)
    private boolean available;

    @Field(type = FieldType.Text)
    private String searchableText;

    @Field(type = FieldType.Date, format = DateFormat.date_time)
    private Instant updatedAt;

    protected ProductSearchDocument() {
    }

    public ProductSearchDocument(UUID productId, String productName, String productSlug,
                                 String shortDescription, String description, UUID categoryId,
                                 String categoryName, String categorySlug, String status,
                                 String mainImageUrl, BigDecimal minPrice, BigDecimal maxPrice,
                                 int variantCount, boolean available, String searchableText, Instant updatedAt) {
        this.id = productId.toString();
        this.productId = productId;
        this.productName = productName;
        this.productSlug = productSlug;
        this.shortDescription = shortDescription;
        this.description = description;
        this.categoryId = categoryId;
        this.categoryName = categoryName;
        this.categorySlug = categorySlug;
        this.status = status;
        this.mainImageUrl = mainImageUrl;
        this.minPrice = minPrice;
        this.maxPrice = maxPrice;
        this.variantCount = variantCount;
        this.available = available;
        this.searchableText = searchableText;
        this.updatedAt = updatedAt;
    }

    public String getId() {
        return id;
    }

    public UUID getProductId() {
        return productId;
    }

    public String getProductName() {
        return productName;
    }

    public String getProductSlug() {
        return productSlug;
    }

    public String getShortDescription() {
        return shortDescription;
    }

    public String getDescription() {
        return description;
    }

    public UUID getCategoryId() {
        return categoryId;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public String getCategorySlug() {
        return categorySlug;
    }

    public String getStatus() {
        return status;
    }

    public String getMainImageUrl() {
        return mainImageUrl;
    }

    public BigDecimal getMinPrice() {
        return minPrice;
    }

    public BigDecimal getMaxPrice() {
        return maxPrice;
    }

    public int getVariantCount() {
        return variantCount;
    }

    public boolean isAvailable() {
        return available;
    }

    public String getSearchableText() {
        return searchableText;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
