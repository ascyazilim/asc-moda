import { Product, ProductFilters } from '../../types/product';
import { apiClient } from './client';

export type SearchResponse = {
  items: Product[];
  total: number;
};

export const searchApi = {
  async searchProducts(query: string, filters?: ProductFilters): Promise<SearchResponse> {
    const response = await apiClient.get<SearchResponse>('/search/products', {
      params: {
        q: query,
        ...filters,
      },
    });

    return response.data;
  },

  async getSuggestions(query: string): Promise<string[]> {
    const response = await apiClient.get<string[]>('/search/suggestions', {
      params: {
        q: query,
      },
    });

    return response.data;
  },
};

