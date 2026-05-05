import dayjs from 'dayjs';

import {
  CartDto,
  CartItemDto,
  CartSummaryDto,
  CatalogProductDto,
  CategoryDto,
  ProductImageDto,
  ProductSearchDto,
  ProductVariantDto,
  SearchPageDto,
  SpringPage,
} from '../../types/api';
import { Cart, CartItem, CartSummary } from '../../types/cart';
import { Category, Product, ProductColor, ProductVariant } from '../../types/product';
import {
  getCategoryImage,
  getColorValue,
  productPlaceholderImage,
} from '../../utils/storefrontVisuals';

export type PagedResult<T> = {
  items: T[];
  total: number;
  page: number;
  pageSize: number;
  totalPages: number;
  first: boolean;
  last: boolean;
};

export function mapCatalogPage(response: SpringPage<CatalogProductDto>): PagedResult<Product> {
  return {
    items: response.content.map(mapCatalogProduct),
    total: response.totalElements,
    page: response.number + 1,
    pageSize: response.size,
    totalPages: response.totalPages,
    first: response.first,
    last: response.last,
  };
}

export function mapSearchPage(response: SearchPageDto<ProductSearchDto>): PagedResult<Product> {
  return {
    items: response.content.map(mapSearchProduct),
    total: response.totalElements,
    page: response.page + 1,
    pageSize: response.size,
    totalPages: response.totalPages,
    first: response.first,
    last: response.last,
  };
}

export function mapCatalogProduct(dto: CatalogProductDto): Product {
  const images = getProductImages(dto.images);
  const variants = dto.variants.filter((variant) => variant.active).map((variant) => mapVariant(variant, dto));
  const colors = uniqueValues(variants.map((variant) => variant.color).filter(Boolean)).map(
    (name) =>
      ({
        name,
        value: getColorValue(name),
      }) satisfies ProductColor,
  );
  const sizes = uniqueValues(variants.map((variant) => variant.size).filter(Boolean));
  const price = toNumber(dto.basePrice);
  const variantPrices = variants.map((variant) => variant.price);
  const minVariantPrice = variantPrices.length ? Math.min(...variantPrices) : price;
  const maxVariantPrice = variantPrices.length ? Math.max(...variantPrices) : price;

  return {
    id: dto.id,
    slug: dto.slug,
    name: dto.name,
    categorySlug: dto.categorySlug,
    categoryLabel: dto.categoryName,
    price: minVariantPrice,
    maxPrice: maxVariantPrice,
    description: dto.description ?? dto.shortDescription ?? '',
    shortDescription: dto.shortDescription ?? undefined,
    details: buildProductDetails(dto),
    images,
    imageAlt: dto.images.find((image) => image.main)?.altText ?? dto.name,
    colors,
    sizes,
    variants,
    isNew: dto.createdAt ? dayjs(dto.createdAt).isAfter(dayjs().subtract(30, 'day')) : false,
    isFeatured: false,
    available: variants.length > 0,
    stock: variants.length > 0 ? 10 : 0,
    updatedAt: dto.updatedAt,
    createdAt: dto.createdAt,
  };
}

export function mapSearchProduct(dto: ProductSearchDto): Product {
  const price = toNumber(dto.minPrice);
  const maxPrice = dto.maxPrice == null ? price : toNumber(dto.maxPrice);

  return {
    id: dto.productId,
    slug: dto.productSlug,
    name: dto.productName,
    categorySlug: dto.categorySlug,
    categoryLabel: dto.categoryName,
    price,
    maxPrice,
    description: dto.shortDescription ?? '',
    shortDescription: dto.shortDescription ?? undefined,
    details: dto.shortDescription ? [dto.shortDescription] : [],
    images: [dto.mainImageUrl || productPlaceholderImage],
    imageAlt: dto.productName,
    colors: [],
    sizes: [],
    variants: [],
    isNew: dto.updatedAt ? dayjs(dto.updatedAt).isAfter(dayjs().subtract(30, 'day')) : false,
    available: dto.available,
    stock: dto.available ? 10 : 0,
    updatedAt: dto.updatedAt,
  };
}

export function mapCategory(dto: CategoryDto): Category {
  return {
    id: dto.id,
    slug: dto.slug,
    title: dto.name,
    description: dto.description ?? 'Asc Moda koleksiyonundan seçili parçalar.',
    image: getCategoryImage(dto.slug),
    href: `/products?categorySlug=${dto.slug}`,
  };
}

export function mapCart(dto: CartDto): Cart {
  return {
    id: dto.id,
    customerId: dto.customerId,
    status: dto.status,
    currency: dto.currency,
    items: dto.items.map(mapCartItem),
    itemCount: dto.itemCount,
    totalQuantity: dto.totalQuantity,
    totalAmount: toNumber(dto.totalAmount),
    selectedItemCount: dto.selectedItemCount,
    selectedTotal: toNumber(dto.selectedTotal),
  };
}

export function mapCartSummary(dto: CartSummaryDto): CartSummary {
  return {
    id: dto.id,
    customerId: dto.customerId,
    status: dto.status,
    currency: dto.currency,
    itemCount: dto.itemCount,
    totalQuantity: dto.totalQuantity,
    totalAmount: toNumber(dto.totalAmount),
    selectedItemCount: dto.selectedItemCount,
    selectedTotal: toNumber(dto.selectedTotal),
  };
}

function mapCartItem(dto: CartItemDto): CartItem {
  return {
    id: dto.id,
    productId: dto.productId,
    productVariantId: dto.productVariantId,
    sku: dto.sku,
    productName: dto.productNameSnapshot,
    productSlug: dto.productSlugSnapshot,
    variantName: dto.variantNameSnapshot ?? undefined,
    imageUrl: dto.mainImageUrlSnapshot ?? productPlaceholderImage,
    color: dto.colorSnapshot ?? undefined,
    size: dto.sizeSnapshot ?? undefined,
    unitPrice: toNumber(dto.unitPriceSnapshot),
    quantity: dto.quantity,
    selected: dto.selected,
    lineTotal: toNumber(dto.lineTotal),
  };
}

function mapVariant(dto: ProductVariantDto, product: CatalogProductDto): ProductVariant {
  return {
    id: dto.id,
    sku: dto.sku,
    color: dto.color ?? undefined,
    size: dto.size ?? undefined,
    price: dto.priceOverride == null ? toNumber(product.basePrice) : toNumber(dto.priceOverride),
    active: dto.active,
    stockKeepingNote: dto.stockKeepingNote ?? undefined,
  };
}

function getProductImages(images: ProductImageDto[]) {
  const activeImages = images
    .filter((image) => image.active)
    .sort((a, b) => {
      if (a.main !== b.main) {
        return a.main ? -1 : 1;
      }

      return a.sortOrder - b.sortOrder;
    })
    .map((image) => image.imageUrl)
    .filter(Boolean);

  return activeImages.length ? activeImages : [productPlaceholderImage];
}

function buildProductDetails(dto: CatalogProductDto) {
  const details = [dto.shortDescription, dto.description].filter(
    (value): value is string => Boolean(value && value.trim()),
  );

  return details.length ? details : ['Ürün detayları katalog verisiyle güncellenecek.'];
}

function uniqueValues(values: Array<string | undefined>) {
  return Array.from(new Set(values.filter((value): value is string => Boolean(value))));
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

