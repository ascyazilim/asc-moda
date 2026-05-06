export type OrderStatus = 'PENDING' | 'CONFIRMED' | 'CANCELLED' | 'FAILED' | string;

export type OrderAddressSnapshot = {
  fullName: string;
  phoneNumber: string;
  city: string;
  district: string;
  addressLine: string;
  postalCode?: string;
  country: string;
};

export type OrderCustomerSnapshot = {
  fullName: string;
  phoneNumber: string;
};

export type OrderItem = {
  id: string;
  cartItemId?: string;
  productId: string;
  productVariantId: string;
  sku: string;
  productName: string;
  productSlug: string;
  imageUrl?: string;
  color?: string;
  size?: string;
  unitPrice: number;
  quantity: number;
  lineTotal: number;
  reservationStatus?: string;
};

export type OrderSummary = {
  id: string;
  orderNumber: string;
  customerId: string;
  externalReference?: string;
  status: OrderStatus;
  currency: string;
  totalAmount: number;
  itemCount: number;
  placedAt?: string;
  confirmedAt?: string;
  cancelledAt?: string;
};

export type OrderDetail = OrderSummary & {
  sourceCartId?: string;
  idempotencyKey?: string;
  paymentReference?: string;
  subtotalAmount: number;
  discountAmount: number;
  shippingAmount: number;
  note?: string;
  source?: string;
  shippingAddress?: OrderAddressSnapshot;
  customerSnapshot?: OrderCustomerSnapshot;
  items: OrderItem[];
  cancellationReason?: string;
  failureReason?: string;
  createdAt?: string;
  updatedAt?: string;
};

export type PagedOrders = {
  items: OrderSummary[];
  total: number;
  page: number;
  pageSize: number;
  totalPages: number;
  first: boolean;
  last: boolean;
};
