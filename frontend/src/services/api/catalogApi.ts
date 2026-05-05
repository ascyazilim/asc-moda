import { SpringPage, CatalogProductDto, CategoryDto } from '../../types/api';
import { Category, Product, ProductFilters, ProductSort } from '../../types/product';
import { apiClient } from './client';
import { mapCatalogPage, mapCatalogProduct, mapCategory, PagedResult } from './mappers';

export type CatalogListResponse = PagedResult<Product>;

export const catalogApi = {
  async getProducts(params?: ProductFilters): Promise<CatalogListResponse> {
    const response = await apiClient.get<SpringPage<CatalogProductDto>>('/api/v1/catalog/products', {
      params: toCatalogParams(params),
    });

    return mapCatalogPage(response.data);
  },

  async getProductBySlug(slug: string): Promise<Product> {
    const response = await apiClient.get<CatalogProductDto>(`/api/v1/catalog/products/${slug}`);

    return mapCatalogProduct(response.data);
  },

  async getCategories(): Promise<Category[]> {
    const response = await apiClient.get<CategoryDto[]>('/api/v1/catalog/categories');

    return response.data.map(mapCategory);
  },

  async getFeaturedProducts(): Promise<Product[]> {
    const response = await this.getProducts({
      sort: 'featured',
      page: 1,
      size: 4,
    });

    return response.items;
  },

  async getNewArrivals(): Promise<Product[]> {
    const response = await this.getProducts({
      sort: 'newest',
      page: 1,
      size: 4,
    });

    return response.items;
  },
};

function toCatalogParams(filters?: ProductFilters) {
  const categorySlug = filters?.categorySlug && filters.categorySlug !== 'all' ? filters.categorySlug : undefined;

  return {
    categorySlug,
    q: filters?.query || undefined,
    page: Math.max((filters?.page ?? 1) - 1, 0),
    size: filters?.size ?? 20,
    sort: mapCatalogSort(filters?.sort),
  };
}

function mapCatalogSort(sort: ProductSort = 'featured') {
  if (sort === 'priceAsc') {
    return 'basePrice,asc';
  }

  if (sort === 'priceDesc') {
    return 'basePrice,desc';
  }

  return 'createdAt,desc';
}
