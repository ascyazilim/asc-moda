import {
  OrderAddressSnapshot,
  OrderDetail,
  OrderItem,
  OrderSummary,
  PagedOrders,
} from '../../types/order';
import { productPlaceholderImage } from '../../utils/storefrontVisuals';
import { apiClient } from './client';

type PageResponseDto<T> = {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
};

type OrderSummaryDto = {
  id: string;
  orderNumber: string;
  customerId: string;
  externalReference?: string | null;
  status: string;
  currency: string;
  totalAmount: string | number;
  itemCount: number;
  placedAt?: string | null;
  confirmedAt?: string | null;
  cancelledAt?: string | null;
};

type OrderResponseDto = OrderSummaryDto & {
  sourceCartId?: string | null;
  idempotencyKey?: string | null;
  paymentReference?: string | null;
  subtotalAmount: string | number;
  discountAmount: string | number;
  shippingAmount: string | number;
  note?: string | null;
  source?: string | null;
  shippingAddress?: OrderAddressSnapshot | null;
  customerSnapshot?: {
    fullName: string;
    phoneNumber: string;
  } | null;
  items: OrderItemResponseDto[];
  cancellationReason?: string | null;
  failureReason?: string | null;
  createdAt?: string;
  updatedAt?: string;
};

type OrderItemResponseDto = {
  id: string;
  cartItemId?: string | null;
  productId: string;
  productVariantId: string;
  sku: string;
  productNameSnapshot: string;
  productSlugSnapshot: string;
  mainImageUrlSnapshot?: string | null;
  colorSnapshot?: string | null;
  sizeSnapshot?: string | null;
  unitPriceSnapshot: string | number;
  quantity: number;
  lineTotal: string | number;
  reservationStatus?: string | null;
};

export const orderApi = {
  async listCustomerOrders(customerId: string, page = 1, size = 12): Promise<PagedOrders> {
    const response = await apiClient.get<PageResponseDto<OrderSummaryDto>>(
      `/api/v1/orders/customer/${customerId}`,
      {
        params: {
          page: Math.max(page - 1, 0),
          size,
        },
      },
    );

    return {
      items: response.data.content.map(mapOrderSummary),
      total: response.data.totalElements,
      page: response.data.page + 1,
      pageSize: response.data.size,
      totalPages: response.data.totalPages,
      first: response.data.first,
      last: response.data.last,
    };
  },

  async getOrder(orderId: string): Promise<OrderDetail> {
    const response = await apiClient.get<OrderResponseDto>(`/api/v1/orders/${orderId}`);

    return mapOrderDetail(response.data);
  },
};

function mapOrderSummary(dto: OrderSummaryDto): OrderSummary {
  return {
    id: dto.id,
    orderNumber: dto.orderNumber,
    customerId: dto.customerId,
    externalReference: dto.externalReference ?? undefined,
    status: dto.status,
    currency: dto.currency,
    totalAmount: toNumber(dto.totalAmount),
    itemCount: dto.itemCount,
    placedAt: dto.placedAt ?? undefined,
    confirmedAt: dto.confirmedAt ?? undefined,
    cancelledAt: dto.cancelledAt ?? undefined,
  };
}

function mapOrderDetail(dto: OrderResponseDto): OrderDetail {
  return {
    ...mapOrderSummary(dto),
    sourceCartId: dto.sourceCartId ?? undefined,
    idempotencyKey: dto.idempotencyKey ?? undefined,
    paymentReference: dto.paymentReference ?? undefined,
    subtotalAmount: toNumber(dto.subtotalAmount),
    discountAmount: toNumber(dto.discountAmount),
    shippingAmount: toNumber(dto.shippingAmount),
    note: dto.note ?? undefined,
    source: dto.source ?? undefined,
    shippingAddress: dto.shippingAddress ?? undefined,
    customerSnapshot: dto.customerSnapshot ?? undefined,
    items: dto.items.map(mapOrderItem),
    cancellationReason: dto.cancellationReason ?? undefined,
    failureReason: dto.failureReason ?? undefined,
    createdAt: dto.createdAt,
    updatedAt: dto.updatedAt,
  };
}

function mapOrderItem(dto: OrderItemResponseDto): OrderItem {
  return {
    id: dto.id,
    cartItemId: dto.cartItemId ?? undefined,
    productId: dto.productId,
    productVariantId: dto.productVariantId,
    sku: dto.sku,
    productName: dto.productNameSnapshot,
    productSlug: dto.productSlugSnapshot,
    imageUrl: dto.mainImageUrlSnapshot ?? productPlaceholderImage,
    color: dto.colorSnapshot ?? undefined,
    size: dto.sizeSnapshot ?? undefined,
    unitPrice: toNumber(dto.unitPriceSnapshot),
    quantity: dto.quantity,
    lineTotal: toNumber(dto.lineTotal),
    reservationStatus: dto.reservationStatus ?? undefined,
  };
}

function toNumber(value: string | number | null | undefined) {
  if (typeof value === 'number') {
    return value;
  }

  if (!value) {
    return 0;
  }

  return Number(value);
}
