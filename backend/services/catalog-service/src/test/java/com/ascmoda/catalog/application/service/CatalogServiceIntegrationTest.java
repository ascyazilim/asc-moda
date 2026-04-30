package com.ascmoda.catalog.application.service;

import com.ascmoda.catalog.api.dto.CategoryResponse;
import com.ascmoda.catalog.api.dto.CreateCategoryRequest;
import com.ascmoda.catalog.api.dto.CreateProductRequest;
import com.ascmoda.catalog.api.dto.CreateProductVariantRequest;
import com.ascmoda.catalog.api.dto.ProductResponse;
import com.ascmoda.catalog.api.error.DuplicateResourceException;
import com.ascmoda.catalog.domain.model.ProductStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(properties = {
        "debug=false",
        "spring.cloud.config.enabled=false",
        "spring.cloud.discovery.enabled=false",
        "spring.cloud.service-registry.auto-registration.enabled=false",
        "spring.config.import=",
        "eureka.client.enabled=false",
        "eureka.client.register-with-eureka=false",
        "eureka.client.fetch-registry=false",
        "spring.jpa.open-in-view=false",
        "ascmoda.catalog.config-source=test"
})
@Testcontainers(disabledWithoutDocker = true)
@Transactional
class CatalogServiceIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private ProductService productService;

    @DynamicPropertySource
    static void postgresProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        registry.add("spring.flyway.enabled", () -> "true");
    }

    @Test
    void createsAndListsCategories() {
        CategoryResponse active = categoryService.create(category("Bags", null, true));
        CategoryResponse inactive = categoryService.create(category("Archived", "archived", false));

        assertThat(categoryService.listActive())
                .extracting(CategoryResponse::id)
                .contains(active.id())
                .doesNotContain(inactive.id());

        assertThat(categoryService.listAllForAdmin(null))
                .extracting(CategoryResponse::id)
                .contains(active.id(), inactive.id());
    }

    @Test
    void createsProductWithGeneratedSlug() {
        CategoryResponse category = categoryService.create(category("Shoes", null, true));

        ProductResponse product = productService.create(product(
                "Leather Sneaker",
                null,
                "SKU-CREATE-1",
                ProductStatus.ACTIVE,
                category.id()
        ));

        assertThat(product.slug()).isEqualTo("leather-sneaker");
        assertThat(product.categorySlug()).isEqualTo(category.slug());
        assertThat(product.variants()).hasSize(1);
    }

    @Test
    void publicListReturnsOnlyActiveProductsInActiveCategories() {
        CategoryResponse activeCategory = categoryService.create(category("Public Bags", null, true));
        CategoryResponse inactiveCategory = categoryService.create(category("Hidden Bags", null, false));

        ProductResponse visible = productService.create(product(
                "Visible Bag",
                "visible-bag",
                "SKU-PUBLIC-1",
                ProductStatus.ACTIVE,
                activeCategory.id()
        ));
        productService.create(product("Inactive Bag", "inactive-bag", "SKU-PUBLIC-2", ProductStatus.INACTIVE, activeCategory.id()));
        productService.create(product("Hidden Category Bag", "hidden-category-bag", "SKU-PUBLIC-3", ProductStatus.ACTIVE, inactiveCategory.id()));

        Page<ProductResponse> products = productService.listPublic(
                null,
                null,
                PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt"))
        );

        assertThat(products.getContent())
                .extracting(ProductResponse::id)
                .containsExactly(visible.id());

        Page<ProductResponse> filtered = productService.listPublic(
                activeCategory.slug(),
                "visible",
                PageRequest.of(0, 20, Sort.by(Sort.Direction.ASC, "name"))
        );

        assertThat(filtered.getContent())
                .extracting(ProductResponse::id)
                .containsExactly(visible.id());
    }

    @Test
    void rejectsDuplicateSlugAndSku() {
        CategoryResponse category = categoryService.create(category("Duplicate Checks", null, true));

        productService.create(product("First Slug", "same-product", "SKU-DUP-1", ProductStatus.ACTIVE, category.id()));

        assertThatThrownBy(() -> productService.create(product(
                "Second Slug",
                "same-product",
                "SKU-DUP-2",
                ProductStatus.ACTIVE,
                category.id()
        ))).isInstanceOf(DuplicateResourceException.class);

        assertThatThrownBy(() -> productService.create(product(
                "Second Sku",
                "second-sku",
                "SKU-DUP-1",
                ProductStatus.ACTIVE,
                category.id()
        ))).isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    void getsActiveProductDetailBySlug() {
        CategoryResponse category = categoryService.create(category("Details", null, true));
        ProductResponse created = productService.create(product(
                "Detail Product",
                "detail-product",
                "SKU-DETAIL-1",
                ProductStatus.ACTIVE,
                category.id()
        ));

        ProductResponse found = productService.getPublicBySlug("detail-product");

        assertThat(found.id()).isEqualTo(created.id());
        assertThat(found.status()).isEqualTo(ProductStatus.ACTIVE);
        assertThat(found.slug()).isEqualTo("detail-product");
    }

    private CreateCategoryRequest category(String name, String slug, boolean active) {
        return new CreateCategoryRequest(name, slug, name + " description", active);
    }

    private CreateProductRequest product(String name, String slug, String sku, ProductStatus status,
                                         java.util.UUID categoryId) {
        return new CreateProductRequest(
                name,
                slug,
                name + " long description",
                name + " short description",
                BigDecimal.valueOf(1299, 2),
                status,
                categoryId,
                List.of(new CreateProductVariantRequest(null, sku, "Black", "M", null, null, true)),
                List.of()
        );
    }
}
