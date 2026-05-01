package com.ascmoda.order.controller;

import com.ascmoda.order.application.service.OrderService;
import com.ascmoda.order.controller.dto.CancelOrderRequest;
import com.ascmoda.order.controller.dto.OrderListItemResponse;
import com.ascmoda.order.controller.dto.OrderResponse;
import com.ascmoda.order.controller.dto.PageResponse;
import com.ascmoda.order.domain.model.OrderStatus;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.math.BigDecimal;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/orders")
public class AdminOrderController {

    private final OrderService orderService;

    public AdminOrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping
    public PageResponse<OrderListItemResponse> list(
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(required = false) UUID customerId,
            @RequestParam(required = false) String orderNumber,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant createdFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant createdTo,
            @RequestParam(required = false) BigDecimal totalMin,
            @RequestParam(required = false) BigDecimal totalMax,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return orderService.listAdmin(status, customerId, orderNumber, createdFrom, createdTo, totalMin, totalMax, pageable);
    }

    @GetMapping("/{orderId}")
    public OrderResponse get(@PathVariable UUID orderId) {
        return orderService.getOrder(orderId);
    }

    @PatchMapping("/{orderId}/confirm")
    public OrderResponse confirm(@PathVariable UUID orderId) {
        return orderService.confirm(orderId);
    }

    @PatchMapping("/{orderId}/cancel")
    public OrderResponse cancel(@PathVariable UUID orderId, @Valid @RequestBody CancelOrderRequest request) {
        return orderService.cancel(orderId, request);
    }
}
