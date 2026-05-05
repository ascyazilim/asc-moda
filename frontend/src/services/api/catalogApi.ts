import { Product, ProductFilters } from '../../types/product';
import { apiClient } from './client';

export type CatalogListResponse = {
  items: Product[];
  total: number;
  page: number;
  pageSize: number;
};

export const catalogApi = {
  async getProducts(params?: ProductFilters): Promise<CatalogListResponse> {
    const response = await apiClient.get<CatalogListResponse>('/catalog/products', {
      params,
    });

    return response.data;
  },

  async getProductBySlug(slug: string): Promise<Product> {
    const response = await apiClient.get<Product>(`/catalog/products/${slug}`);

    return response.data;
  },

  async getFeaturedProducts(): Promise<Product[]> {
    const response = await apiClient.get<Product[]>('/catalog/products/featured');

    return response.data;
  },

  async getNewArrivals(): Promise<Product[]> {
    const response = await apiClient.get<Product[]>('/catalog/products/new-arrivals');

    return response.data;
  },
};

