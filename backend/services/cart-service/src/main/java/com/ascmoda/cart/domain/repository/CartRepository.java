package com.ascmoda.cart.domain.repository;

import com.ascmoda.cart.domain.model.Cart;
import com.ascmoda.cart.domain.model.CartStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface CartRepository extends JpaRepository<Cart, UUID> {

    @EntityGraph(attributePaths = "items")
    Optional<Cart> findByCustomerIdAndStatus(UUID customerId, CartStatus status);

    @EntityGraph(attributePaths = "items")
    Optional<Cart> findByIdAndCustomerId(UUID id, UUID customerId);

    @EntityGraph(attributePaths = "items")
    Optional<Cart> findWithItemsById(UUID id);

    @Query("""
            select c
            from Cart c
            where (:status is null or c.status = :status)
              and (:customerId is null or c.customerId = :customerId)
              and (:createdFrom is null or c.createdAt >= :createdFrom)
              and (:createdTo is null or c.createdAt <= :createdTo)
            """)
    Page<Cart> searchAdmin(
            @Param("status") CartStatus status,
            @Param("customerId") UUID customerId,
            @Param("createdFrom") Instant createdFrom,
            @Param("createdTo") Instant createdTo,
            Pageable pageable
    );

    boolean existsByCustomerIdAndStatus(UUID customerId, CartStatus status);
}
