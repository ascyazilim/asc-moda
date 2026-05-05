export type ProductCategory =
  | 'basortu'
  | 'sal'
  | 'elbise'
  | 'etek'
  | 'bluz'
  | 'dis-giyim'
  | 'tunik';

export type ProductColor = {
  name: string;
  value: string;
};

export type Product = {
  id: string;
  slug: string;
  name: string;
  category: ProductCategory;
  categoryLabel: string;
  price: number;
  compareAtPrice?: number;
  description: string;
  details: string[];
  images: string[];
  colors: ProductColor[];
  sizes: string[];
  isNew?: boolean;
  isFeatured?: boolean;
  stock: number;
};

export type ProductFilters = {
  category?: ProductCategory | 'all';
  priceRange?: [number, number];
  colors?: string[];
  sizes?: string[];
  sort?: ProductSort;
  page?: number;
  query?: string;
};

export type ProductSort = 'featured' | 'newest' | 'priceAsc' | 'priceDesc';

export type Category = {
  id: ProductCategory;
  title: string;
  description: string;
  image: string;
  href: string;
};

