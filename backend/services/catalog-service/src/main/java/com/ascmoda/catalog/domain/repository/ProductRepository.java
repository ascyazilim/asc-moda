package com.ascmoda.catalog.domain.repository;

import com.ascmoda.catalog.domain.model.Product;
import com.ascmoda.catalog.domain.model.ProductStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface ProductRepository extends JpaRepository<Product, UUID> {

    @EntityGraph(attributePaths = "category")
    Optional<Product> findBySlug(String slug);

    @EntityGraph(attributePaths = "category")
    @Query("""
            select p
            from Product p
            join p.category c
            where p.slug = :slug
              and p.status = :status
              and c.active = true
            """)
    Optional<Product> findPublicBySlug(@Param("slug") String slug, @Param("status") ProductStatus status);

    @EntityGraph(attributePaths = "category")
    @Query("""
            select p
            from Product p
            join p.category c
            where p.status = :status
              and c.active = true
              and (:categorySlug is null or c.slug = :categorySlug)
              and (
                    :q = ''
                    or lower(p.name) like concat('%', :q, '%')
                    or lower(p.shortDescription) like concat('%', :q, '%')
                    or lower(p.description) like concat('%', :q, '%')
              )
            """)
    Page<Product> searchPublic(
            @Param("status") ProductStatus status,
            @Param("categorySlug") String categorySlug,
            @Param("q") String q,
            Pageable pageable
    );

    @EntityGraph(attributePaths = "category")
    @Query("""
            select p
            from Product p
            where (:status is null or p.status = :status)
            """)
    Page<Product> searchAdmin(@Param("status") ProductStatus status, Pageable pageable);

    boolean existsBySlug(String slug);

    boolean existsBySlugAndIdNot(String slug, UUID id);
}
