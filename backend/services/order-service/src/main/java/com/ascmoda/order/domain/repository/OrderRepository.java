package com.ascmoda.order.domain.repository;

import com.ascmoda.order.domain.model.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.lang.Nullable;

import java.util.Optional;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<Order, UUID>, JpaSpecificationExecutor<Order> {

    @EntityGraph(attributePaths = "items")
    Optional<Order> findWithItemsById(UUID id);

    @EntityGraph(attributePaths = "items")
    Optional<Order> findWithItemsByIdAndCustomerId(UUID id, UUID customerId);

    @EntityGraph(attributePaths = "items")
    Optional<Order> findWithItemsBySourceCartId(UUID sourceCartId);

    @EntityGraph(attributePaths = "items")
    Optional<Order> findWithItemsByCustomerIdAndIdempotencyKey(UUID customerId, String idempotencyKey);

    boolean existsByOrderNumber(String orderNumber);

    @EntityGraph(attributePaths = "items")
    Page<Order> findByCustomerId(UUID customerId, Pageable pageable);

    @Override
    @EntityGraph(attributePaths = "items")
    Page<Order> findAll(@Nullable Specification<Order> specification, Pageable pageable);
}
