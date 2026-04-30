package com.ascmoda.cart.application.mapper;

import com.ascmoda.cart.api.dto.CartItemResponse;
import com.ascmoda.cart.api.dto.CartResponse;
import com.ascmoda.cart.api.dto.CartSummaryResponse;
import com.ascmoda.cart.domain.model.Cart;
import com.ascmoda.cart.domain.model.CartItem;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;

@Component
public class CartMapper {

    private final CartItemMapper cartItemMapper;

    public CartMapper(CartItemMapper cartItemMapper) {
        this.cartItemMapper = cartItemMapper;
    }

    public CartResponse toResponse(Cart cart) {
        List<CartItemResponse> items = cart.getItems()
                .stream()
                .sorted(Comparator.comparing(CartItem::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())))
                .map(cartItemMapper::toResponse)
                .toList();

        return new CartResponse(
                cart.getId(),
                cart.getCustomerId(),
                cart.getStatus(),
                cart.getCurrency(),
                items,
                cart.getItems().size(),
                totalQuantity(cart),
                totalAmount(cart),
                selectedItemCount(cart),
                selectedTotal(cart),
                cart.getVersion(),
                cart.getCreatedAt(),
                cart.getUpdatedAt(),
                cart.getLastActivityAt(),
                cart.getCheckedOutAt(),
                cart.getAbandonedAt()
        );
    }

    public CartSummaryResponse toSummaryResponse(Cart cart) {
        return new CartSummaryResponse(
                cart.getId(),
                cart.getCustomerId(),
                cart.getStatus(),
                cart.getCurrency(),
                cart.getItems().size(),
                totalQuantity(cart),
                totalAmount(cart),
                selectedItemCount(cart),
                selectedTotal(cart)
        );
    }

    public int selectedItemCount(Cart cart) {
        return (int) cart.getItems()
                .stream()
                .filter(CartItem::isSelected)
                .count();
    }

    public BigDecimal selectedTotal(Cart cart) {
        return cart.getItems()
                .stream()
                .filter(CartItem::isSelected)
                .map(CartItem::lineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal totalAmount(Cart cart) {
        return cart.getItems()
                .stream()
                .map(CartItem::lineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private int totalQuantity(Cart cart) {
        return cart.getItems()
                .stream()
                .mapToInt(CartItem::getQuantity)
                .sum();
    }
}
