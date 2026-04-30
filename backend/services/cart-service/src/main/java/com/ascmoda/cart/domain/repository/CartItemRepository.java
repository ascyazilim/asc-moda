package com.ascmoda.cart.domain.repository;

import com.ascmoda.cart.domain.model.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CartItemRepository extends JpaRepository<CartItem, UUID> {

    Optional<CartItem> findByCartIdAndProductVariantId(UUID cartId, UUID productVariantId);

    Optional<CartItem> findByIdAndCartId(UUID id, UUID cartId);
}
