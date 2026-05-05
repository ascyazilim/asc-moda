export type ProductCategory = string;

export type ProductColor = {
  name: string;
  value: string;
};

export type ProductVariant = {
  id: string;
  sku: string;
  color?: string;
  size?: string;
  price: number;
  active: boolean;
  stockKeepingNote?: string;
};

export type Product = {
  id: string;
  slug: string;
  name: string;
  categorySlug: ProductCategory;
  categoryLabel: string;
  price: number;
  maxPrice?: number;
  compareAtPrice?: number;
  description: string;
  shortDescription?: string;
  details: string[];
  images: string[];
  imageAlt?: string;
  colors: ProductColor[];
  sizes: string[];
  variants: ProductVariant[];
  isNew?: boolean;
  isFeatured?: boolean;
  available?: boolean;
  stock?: number;
  updatedAt?: string;
  createdAt?: string;
};

export type ProductFilters = {
  categorySlug?: ProductCategory | 'all';
  priceRange?: [number, number];
  colors?: string[];
  sizes?: string[];
  sort?: ProductSort;
  page?: number;
  size?: number;
  query?: string;
};

export type ProductSort = 'featured' | 'newest' | 'priceAsc' | 'priceDesc';

export type Category = {
  id: string;
  slug: string;
  title: string;
  description: string;
  image: string;
  href: string;
};
