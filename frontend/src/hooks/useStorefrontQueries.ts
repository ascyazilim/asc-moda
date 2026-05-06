import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';

import { useAuth } from '../app/auth/AuthContext';
import { cartApi, AddCartItemPayload } from '../services/api/cartApi';
import { catalogApi } from '../services/api/catalogApi';
import { resolveCustomerId } from '../services/api/customerIdentity';
import { queryKeys } from '../services/api/queryKeys';
import { searchApi } from '../services/api/searchApi';
import { ProductFilters } from '../types/product';
import { useCurrentCustomer } from './useAccountQueries';

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

export function useCart() {
  const { isAuthenticated } = useAuth();
  const currentCustomerQuery = useCurrentCustomer();
  const customerId = resolveCustomerId(currentCustomerQuery.data);

  return useQuery({
    queryKey: queryKeys.cart(customerId ?? 'guest'),
    enabled: isAuthenticated && Boolean(customerId),
    queryFn: () => cartApi.getCart(requireCustomerId(customerId)),
  });
}

export function useCartSummary() {
  const { isAuthenticated } = useAuth();
  const currentCustomerQuery = useCurrentCustomer();
  const customerId = resolveCustomerId(currentCustomerQuery.data);

  return useQuery({
    queryKey: queryKeys.cartSummary(customerId ?? 'guest'),
    enabled: isAuthenticated && Boolean(customerId),
    queryFn: () => cartApi.getSummary(requireCustomerId(customerId)),
    retry: false,
  });
}

export function useAddCartItemMutation() {
  const queryClient = useQueryClient();
  const currentCustomerQuery = useCurrentCustomer();
  const customerId = resolveCustomerId(currentCustomerQuery.data);

  return useMutation({
    mutationFn: (payload: AddCartItemPayload) => cartApi.addItem(payload, requireCustomerId(customerId)),
    onSuccess: () => {
      invalidateCartQueries(queryClient, requireCustomerId(customerId));
    },
  });
}

export function useUpdateCartItemQuantityMutation() {
  const queryClient = useQueryClient();
  const currentCustomerQuery = useCurrentCustomer();
  const customerId = resolveCustomerId(currentCustomerQuery.data);

  return useMutation({
    mutationFn: ({ itemId, quantity }: { itemId: string; quantity: number }) =>
      cartApi.updateQuantity(itemId, quantity, requireCustomerId(customerId)),
    onSuccess: () => {
      invalidateCartQueries(queryClient, requireCustomerId(customerId));
    },
  });
}

export function useRemoveCartItemMutation() {
  const queryClient = useQueryClient();
  const currentCustomerQuery = useCurrentCustomer();
  const customerId = resolveCustomerId(currentCustomerQuery.data);

  return useMutation({
    mutationFn: (itemId: string) => cartApi.removeItem(itemId, requireCustomerId(customerId)),
    onSuccess: () => {
      invalidateCartQueries(queryClient, requireCustomerId(customerId));
    },
  });
}

export function useClearCartMutation() {
  const queryClient = useQueryClient();
  const currentCustomerQuery = useCurrentCustomer();
  const customerId = resolveCustomerId(currentCustomerQuery.data);

  return useMutation({
    mutationFn: () => cartApi.clearCart(requireCustomerId(customerId)),
    onSuccess: () => {
      invalidateCartQueries(queryClient, requireCustomerId(customerId));
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

function requireCustomerId(customerId: string | undefined) {
  if (!customerId) {
    throw new Error('Sepet icin musteri oturumu hazir degil.');
  }

  return customerId;
}
