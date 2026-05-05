export type CartItem = {
  id: string;
  productId: string;
  productVariantId: string;
  sku: string;
  productName: string;
  productSlug: string;
  variantName?: string;
  imageUrl?: string;
  color?: string;
  size?: string;
  unitPrice: number;
  quantity: number;
  selected: boolean;
  lineTotal: number;
};

export type Cart = {
  id: string;
  customerId: string;
  status: string;
  currency: string;
  items: CartItem[];
  itemCount: number;
  totalQuantity: number;
  totalAmount: number;
  selectedItemCount: number;
  selectedTotal: number;
};

export type CartTotals = {
  subtotal: number;
  discount: number;
  shipping: number;
  total: number;
};

export type CartSummary = {
  id?: string;
  customerId?: string;
  status?: string;
  currency?: string;
  itemCount: number;
  totalQuantity: number;
  totalAmount: number;
  selectedItemCount: number;
  selectedTotal: number;
};
