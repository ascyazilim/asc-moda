import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';

import { cartApi, AddCartItemPayload } from '../services/api/cartApi';
import { catalogApi } from '../services/api/catalogApi';
import { apiConfig } from '../services/api/config';
import { queryKeys } from '../services/api/queryKeys';
import { searchApi } from '../services/api/searchApi';
import { ProductFilters } from '../types/product';

export function useProducts(filters?: ProductFilters) {
  return useQuery({
    queryKey: queryKeys.productList(filters),
    queryFn: () => searchApi.searchProducts(filters?.query ?? '', filters),
  });
}

export function useFeaturedProducts() {
  return useQuery({
    queryKey: queryKeys.featuredProducts,
    queryFn: () => catalogApi.getFeaturedProducts(),
  });
}

export function useNewArrivals() {
  return useQuery({
    queryKey: queryKeys.newArrivals,
    queryFn: () => catalogApi.getNewArrivals(),
  });
}

export function useCategories() {
  return useQuery({
    queryKey: queryKeys.categories,
    queryFn: () => catalogApi.getCategories(),
  });
}

export function useProductDetail(slug: string | undefined) {
  return useQuery({
    queryKey: queryKeys.productDetail(slug ?? ''),
    enabled: Boolean(slug),
    queryFn: () => {
      if (!slug) {
        throw new Error('Ürün adresi bulunamadı.');
      }

      return catalogApi.getProductBySlug(slug);
    },
  });
}

export function useSearchResults(query: string, filters?: ProductFilters) {
  return useQuery({
    queryKey: queryKeys.search(query, filters),
    queryFn: () => searchApi.searchProducts(query, filters),
  });
}

export function useCart(customerId = apiConfig.demoCustomerId) {
  return useQuery({
    queryKey: queryKeys.cart(customerId),
    queryFn: () => cartApi.getCart(customerId),
  });
}

export function useCartSummary(customerId = apiConfig.demoCustomerId) {
  return useQuery({
    queryKey: queryKeys.cartSummary(customerId),
    queryFn: () => cartApi.getSummary(customerId),
    retry: false,
  });
}

export function useAddCartItemMutation(customerId = apiConfig.demoCustomerId) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (payload: AddCartItemPayload) => cartApi.addItem(payload, customerId),
    onSuccess: () => {
      invalidateCartQueries(queryClient, customerId);
    },
  });
}

export function useUpdateCartItemQuantityMutation(customerId = apiConfig.demoCustomerId) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ itemId, quantity }: { itemId: string; quantity: number }) =>
      cartApi.updateQuantity(itemId, quantity, customerId),
    onSuccess: () => {
      invalidateCartQueries(queryClient, customerId);
    },
  });
}

export function useRemoveCartItemMutation(customerId = apiConfig.demoCustomerId) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (itemId: string) => cartApi.removeItem(itemId, customerId),
    onSuccess: () => {
      invalidateCartQueries(queryClient, customerId);
    },
  });
}

export function useClearCartMutation(customerId = apiConfig.demoCustomerId) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: () => cartApi.clearCart(customerId),
    onSuccess: () => {
      invalidateCartQueries(queryClient, customerId);
    },
  });
}

function invalidateCartQueries(queryClient: ReturnType<typeof useQueryClient>, customerId: string) {
  queryClient.invalidateQueries({
    queryKey: queryKeys.cart(customerId),
  });
  queryClient.invalidateQueries({
    queryKey: queryKeys.cartSummary(customerId),
  });
}
