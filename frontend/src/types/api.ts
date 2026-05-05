export type SpringPage<T> = {
  content: T[];
  number: number;
  size: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
};

export type SearchPageDto<T> = {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
};

export type ProductImageDto = {
  id: string;
  productId: string;
  variantId?: string | null;
  imageUrl: string;
  altText?: string | null;
  sortOrder: number;
  main: boolean;
  active: boolean;
  createdAt?: string;
  updatedAt?: string;
};

export type ProductVariantDto = {
  id: string;
  sku: string;
  color?: string | null;
  size?: string | null;
  stockKeepingNote?: string | null;
  priceOverride?: string | number | null;
  active: boolean;
  createdAt?: string;
  updatedAt?: string;
};

export type CatalogProductDto = {
  id: string;
  name: string;
  slug: string;
  description?: string | null;
  shortDescription?: string | null;
  basePrice: string | number;
  status: string;
  categoryId: string;
  categoryName: string;
  categorySlug: string;
  variants: ProductVariantDto[];
  images: ProductImageDto[];
  createdAt?: string;
  updatedAt?: string;
};

export type CategoryDto = {
  id: string;
  name: string;
  slug: string;
  description?: string | null;
  active: boolean;
  createdAt?: string;
  updatedAt?: string;
};

export type ProductSearchDto = {
  productId: string;
  productName: string;
  productSlug: string;
  shortDescription?: string | null;
  categoryName: string;
  categorySlug: string;
  mainImageUrl?: string | null;
  minPrice: string | number;
  maxPrice?: string | number | null;
  variantCount: number;
  available: boolean;
  updatedAt?: string;
};

export type CartItemDto = {
  id: string;
  productId: string;
  productVariantId: string;
  sku: string;
  productNameSnapshot: string;
  productSlugSnapshot: string;
  variantNameSnapshot?: string | null;
  mainImageUrlSnapshot?: string | null;
  colorSnapshot?: string | null;
  sizeSnapshot?: string | null;
  unitPriceSnapshot: string | number;
  quantity: number;
  selected: boolean;
  lineTotal: string | number;
  createdAt?: string;
  updatedAt?: string;
};

export type CartDto = {
  id: string;
  customerId: string;
  status: string;
  currency: string;
  items: CartItemDto[];
  itemCount: number;
  totalQuantity: number;
  totalAmount: string | number;
  selectedItemCount: number;
  selectedTotal: string | number;
  createdAt?: string;
  updatedAt?: string;
  lastActivityAt?: string;
};

export type CartSummaryDto = {
  id: string;
  customerId: string;
  status: string;
  currency: string;
  itemCount: number;
  totalQuantity: number;
  totalAmount: string | number;
  selectedItemCount: number;
  selectedTotal: string | number;
};

