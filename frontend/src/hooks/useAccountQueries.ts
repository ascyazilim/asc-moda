import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';

import { useAuth } from '../app/auth/AuthContext';
import { CurrentUser } from '../app/auth/authTypes';
import { customerApi } from '../services/api/customerApi';
import { orderApi } from '../services/api/orderApi';
import { queryKeys } from '../services/api/queryKeys';
import { CustomerAddressInput, UpdateCustomerProfileInput } from '../types/customer';

export function useCurrentCustomer() {
  const { currentUser, isAuthenticated } = useAuth();

  return useQuery({
    queryKey: queryKeys.currentCustomer(currentUser?.sub),
    enabled: isAuthenticated && Boolean(currentUser),
    queryFn: () => customerApi.ensureCurrentCustomer(requireCurrentUser(currentUser)),
    retry: false,
  });
}

export function useUpdateCustomerProfileMutation() {
  const queryClient = useQueryClient();
  const { currentUser } = useAuth();
  const currentCustomerQuery = useCurrentCustomer();
  const customerId = currentCustomerQuery.data?.id;

  return useMutation({
    mutationFn: (payload: UpdateCustomerProfileInput) => {
      if (!customerId) {
        throw new Error('Musteri oturumu hazir degil.');
      }

      return customerApi.updateProfile(customerId, {
        ...payload,
        externalUserId: currentUser?.sub,
      });
    },
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: queryKeys.currentCustomer(currentUser?.sub),
      });
    },
  });
}

export function useCustomerAddresses() {
  const currentCustomerQuery = useCurrentCustomer();
  const customerId = currentCustomerQuery.data?.id;

  return useQuery({
    queryKey: queryKeys.customerAddresses(customerId),
    enabled: Boolean(customerId),
    queryFn: () => customerApi.listAddresses(requireCustomerId(customerId)),
  });
}

export function useAddAddressMutation() {
  const queryClient = useQueryClient();
  const currentCustomerQuery = useCurrentCustomer();
  const customerId = currentCustomerQuery.data?.id;

  return useMutation({
    mutationFn: (payload: CustomerAddressInput) =>
      customerApi.addAddress(requireCustomerId(customerId), payload),
    onSuccess: () => invalidateAddressQueries(queryClient, customerId),
  });
}

export function useUpdateAddressMutation() {
  const queryClient = useQueryClient();
  const currentCustomerQuery = useCurrentCustomer();
  const customerId = currentCustomerQuery.data?.id;

  return useMutation({
    mutationFn: ({ addressId, payload }: { addressId: string; payload: CustomerAddressInput }) =>
      customerApi.updateAddress(requireCustomerId(customerId), addressId, payload),
    onSuccess: () => invalidateAddressQueries(queryClient, customerId),
  });
}

export function useDeactivateAddressMutation() {
  const queryClient = useQueryClient();
  const currentCustomerQuery = useCurrentCustomer();
  const customerId = currentCustomerQuery.data?.id;

  return useMutation({
    mutationFn: (addressId: string) =>
      customerApi.deactivateAddress(requireCustomerId(customerId), addressId),
    onSuccess: () => invalidateAddressQueries(queryClient, customerId),
  });
}

export function useSetDefaultShippingAddressMutation() {
  const queryClient = useQueryClient();
  const currentCustomerQuery = useCurrentCustomer();
  const customerId = currentCustomerQuery.data?.id;

  return useMutation({
    mutationFn: (addressId: string) =>
      customerApi.setDefaultShipping(requireCustomerId(customerId), addressId),
    onSuccess: () => invalidateAddressQueries(queryClient, customerId),
  });
}

export function useSetDefaultBillingAddressMutation() {
  const queryClient = useQueryClient();
  const currentCustomerQuery = useCurrentCustomer();
  const customerId = currentCustomerQuery.data?.id;

  return useMutation({
    mutationFn: (addressId: string) =>
      customerApi.setDefaultBilling(requireCustomerId(customerId), addressId),
    onSuccess: () => invalidateAddressQueries(queryClient, customerId),
  });
}

export function useCustomerOrders(page = 1) {
  const currentCustomerQuery = useCurrentCustomer();
  const customerId = currentCustomerQuery.data?.id;

  return useQuery({
    queryKey: queryKeys.customerOrders(customerId, page),
    enabled: Boolean(customerId),
    queryFn: () => orderApi.listCustomerOrders(requireCustomerId(customerId), page),
  });
}

export function useCustomerOrderDetail(orderId: string | undefined) {
  return useQuery({
    queryKey: queryKeys.orderDetail(orderId),
    enabled: Boolean(orderId),
    queryFn: () => orderApi.getOrder(requireOrderId(orderId)),
  });
}

function invalidateAddressQueries(
  queryClient: ReturnType<typeof useQueryClient>,
  customerId: string | undefined,
) {
  queryClient.invalidateQueries({
    queryKey: queryKeys.customerAddresses(customerId),
  });
  queryClient.invalidateQueries({
    queryKey: ['customer', 'current'],
  });
}

function requireCurrentUser(user: CurrentUser | null) {
  if (!user) {
    throw new Error('Oturum bulunamadi.');
  }

  return user;
}

function requireCustomerId(customerId: string | undefined) {
  if (!customerId) {
    throw new Error('Musteri kimligi hazir degil.');
  }

  return customerId;
}

function requireOrderId(orderId: string | undefined) {
  if (!orderId) {
    throw new Error('Siparis bulunamadi.');
  }

  return orderId;
}
