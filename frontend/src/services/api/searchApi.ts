import { ProductSearchDto, SearchPageDto } from '../../types/api';
import { Product, ProductFilters, ProductSort } from '../../types/product';
import { apiClient } from './client';
import { mapSearchPage, PagedResult } from './mappers';

export type SearchResponse = PagedResult<Product>;

export const searchApi = {
  async searchProducts(query = '', filters?: ProductFilters): Promise<SearchResponse> {
    const response = await apiClient.get<SearchPageDto<ProductSearchDto>>('/api/v1/search/products', {
      params: toSearchParams(query, filters),
    });

    return mapSearchPage(response.data);
  },

  async getProductBySlug(slug: string): Promise<Product> {
    const response = await apiClient.get<ProductSearchDto>(`/api/v1/search/products/${slug}`);

    return mapSearchPage({
      content: [response.data],
      page: 0,
      size: 1,
      totalElements: 1,
      totalPages: 1,
      first: true,
      last: true,
    }).items[0];
  },
};

function toSearchParams(query: string, filters?: ProductFilters) {
  const priceRange = filters?.priceRange;

  return {
    q: query || filters?.query || undefined,
    categorySlug:
      filters?.categorySlug && filters.categorySlug !== 'all' ? filters.categorySlug : undefined,
    minPrice: priceRange ? priceRange[0] : undefined,
    maxPrice: priceRange ? priceRange[1] : undefined,
    sort: mapSearchSort(filters?.sort),
    page: Math.max((filters?.page ?? 1) - 1, 0),
    size: filters?.size ?? 20,
  };
}

function mapSearchSort(sort: ProductSort = 'featured') {
  if (sort === 'featured') {
    return 'relevance';
  }

  return sort;
}
