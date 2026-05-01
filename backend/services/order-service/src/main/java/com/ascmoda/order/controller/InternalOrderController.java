package com.ascmoda.order.controller;

import com.ascmoda.order.application.service.OrderService;
import com.ascmoda.order.controller.dto.OrderResponse;
import com.ascmoda.order.controller.dto.OrderSummaryResponse;
import com.ascmoda.order.controller.dto.PageResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/internal/orders")
public class InternalOrderController {

    private final OrderService orderService;

    public InternalOrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping("/{orderId}")
    public OrderResponse get(@PathVariable UUID orderId) {
        return orderService.getOrder(orderId);
    }

    @GetMapping("/customer/{customerId}")
    public PageResponse<OrderSummaryResponse> listCustomerOrders(
            @PathVariable UUID customerId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return orderService.listCustomerOrders(customerId, pageable);
    }
}
