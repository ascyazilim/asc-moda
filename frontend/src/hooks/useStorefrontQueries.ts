import { useQuery } from '@tanstack/react-query';

import { catalogApi } from '../services/api/catalogApi';
import { queryKeys } from '../services/api/queryKeys';
import { searchApi } from '../services/api/searchApi';
import { ProductFilters } from '../types/product';
import {
  filterProducts,
  getProductBySlug,
  storefrontProducts,
} from '../modules/storefront/mock/storefrontData';

const useMocks = import.meta.env.VITE_USE_MOCKS !== 'false';

export function useProducts(filters?: ProductFilters) {
  return useQuery({
    queryKey: [...queryKeys.products, filters],
    queryFn: async () => {
      if (useMocks) {
        const items = filterProducts(filters);

        return {
          items,
          total: items.length,
          page: filters?.page ?? 1,
          pageSize: 12,
        };
      }

      return catalogApi.getProducts(filters);
    },
  });
}

export function useFeaturedProducts() {
  return useQuery({
    queryKey: queryKeys.featuredProducts,
    queryFn: async () => {
      if (useMocks) {
        return storefrontProducts.filter((product) => product.isFeatured).slice(0, 4);
      }

      return catalogApi.getFeaturedProducts();
    },
  });
}

export function useNewArrivals() {
  return useQuery({
    queryKey: queryKeys.newArrivals,
    queryFn: async () => {
      if (useMocks) {
        return storefrontProducts.filter((product) => product.isNew).slice(0, 4);
      }

      return catalogApi.getNewArrivals();
    },
  });
}

export function useProductDetail(slug: string | undefined) {
  return useQuery({
    queryKey: queryKeys.productDetail(slug ?? ''),
    enabled: Boolean(slug),
    queryFn: async () => {
      if (!slug) {
        throw new Error('Ürün adresi bulunamadı.');
      }

      if (useMocks) {
        const product = getProductBySlug(slug);

        if (!product) {
          throw new Error('Ürün bulunamadı.');
        }

        return product;
      }

      return catalogApi.getProductBySlug(slug);
    },
  });
}

export function useSearchResults(query: string, filters?: ProductFilters) {
  return useQuery({
    queryKey: [...queryKeys.search(query), filters],
    queryFn: async () => {
      if (useMocks) {
        const items = filterProducts({
          ...filters,
          query,
        });

        return {
          items,
          total: items.length,
        };
      }

      return searchApi.searchProducts(query, filters);
    },
  });
}

