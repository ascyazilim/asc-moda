package com.ascmoda.order.application.mapper;

import com.ascmoda.order.controller.dto.AddressSnapshotRequest;
import com.ascmoda.order.controller.dto.AddressSnapshotResponse;
import com.ascmoda.order.controller.dto.CreateOrderRequest;
import com.ascmoda.order.controller.dto.CustomerSnapshotResponse;
import com.ascmoda.order.controller.dto.OrderItemResponse;
import com.ascmoda.order.controller.dto.OrderListItemResponse;
import com.ascmoda.order.controller.dto.OrderResponse;
import com.ascmoda.order.controller.dto.OrderSummaryResponse;
import com.ascmoda.order.controller.dto.PageResponse;
import com.ascmoda.order.domain.model.AddressSnapshot;
import com.ascmoda.order.domain.model.CustomerSnapshot;
import com.ascmoda.order.domain.model.Order;
import com.ascmoda.order.domain.model.OrderItem;
import org.mapstruct.Mapper;
import org.springframework.data.domain.Page;

@Mapper(componentModel = "spring")
public interface OrderMapper {

    AddressSnapshotResponse toAddressSnapshotResponse(AddressSnapshot address);

    CustomerSnapshotResponse toCustomerSnapshotResponse(CustomerSnapshot customer);

    OrderItemResponse toItemResponse(OrderItem item);

    default AddressSnapshot toAddressSnapshot(AddressSnapshotRequest request) {
        return new AddressSnapshot(
                request.fullName(),
                request.phoneNumber(),
                request.city(),
                request.district(),
                request.addressLine(),
                request.postalCode(),
                request.country()
        );
    }

    default CustomerSnapshot toCustomerSnapshot(CreateOrderRequest request) {
        return new CustomerSnapshot(
                request.shippingAddress().fullName(),
                request.shippingAddress().phoneNumber()
        );
    }

    default OrderResponse toResponse(Order order) {
        return new OrderResponse(
                order.getId(),
                order.getOrderNumber(),
                order.getSourceCartId(),
                order.getCustomerId(),
                order.getIdempotencyKey(),
                order.getExternalReference(),
                order.getPaymentReference(),
                order.getStatus(),
                order.getCurrency(),
                order.getSubtotalAmount(),
                order.getDiscountAmount(),
                order.getShippingAmount(),
                order.getTotalAmount(),
                order.getNote(),
                order.getSource(),
                toAddressSnapshotResponse(order.getShippingAddress()),
                toCustomerSnapshotResponse(order.getCustomerSnapshot()),
                order.getItems().stream().map(this::toItemResponse).toList(),
                order.getPlacedAt(),
                order.getConfirmedAt(),
                order.getCancelledAt(),
                order.getCancellationReason(),
                order.getFailureReason(),
                order.getVersion(),
                order.getCreatedAt(),
                order.getUpdatedAt()
        );
    }

    default OrderSummaryResponse toSummaryResponse(Order order) {
        return new OrderSummaryResponse(
                order.getId(),
                order.getOrderNumber(),
                order.getCustomerId(),
                order.getExternalReference(),
                order.getStatus(),
                order.getCurrency(),
                order.getTotalAmount(),
                order.getItems().size(),
                order.getPlacedAt(),
                order.getConfirmedAt(),
                order.getCancelledAt()
        );
    }

    default OrderListItemResponse toListItemResponse(Order order) {
        return new OrderListItemResponse(
                order.getId(),
                order.getOrderNumber(),
                order.getSourceCartId(),
                order.getCustomerId(),
                order.getExternalReference(),
                order.getPaymentReference(),
                order.getStatus(),
                order.getSource(),
                order.getCurrency(),
                order.getTotalAmount(),
                order.getItems().size(),
                order.getPlacedAt(),
                order.getCreatedAt()
        );
    }

    default <T> PageResponse<T> toPageResponse(Page<T> page) {
        return new PageResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast()
        );
    }
}
