import { CartDto, CartSummaryDto } from '../../types/api';
import { Cart, CartSummary } from '../../types/cart';
import { apiClient } from './client';
import { apiConfig } from './config';
import { mapCart, mapCartSummary } from './mappers';

export type AddCartItemPayload = {
  productVariantId: string;
  sku: string;
  quantity: number;
};

export const cartApi = {
  async getCart(customerId = apiConfig.demoCustomerId): Promise<Cart> {
    const response = await apiClient.get<CartDto>(`/api/v1/carts/${customerId}`);

    return mapCart(response.data);
  },

  async getSummary(customerId = apiConfig.demoCustomerId): Promise<CartSummary> {
    const response = await apiClient.get<CartSummaryDto>(`/api/v1/carts/${customerId}/summary`);

    return mapCartSummary(response.data);
  },

  async addItem(payload: AddCartItemPayload, customerId = apiConfig.demoCustomerId): Promise<Cart> {
    const response = await apiClient.post<CartDto>(`/api/v1/carts/${customerId}/items`, payload);

    return mapCart(response.data);
  },

  async updateQuantity(
    itemId: string,
    quantity: number,
    customerId = apiConfig.demoCustomerId,
  ): Promise<Cart> {
    const response = await apiClient.patch<CartDto>(`/api/v1/carts/${customerId}/items/${itemId}/quantity`, {
      quantity,
    });

    return mapCart(response.data);
  },

  async removeItem(itemId: string, customerId = apiConfig.demoCustomerId): Promise<Cart> {
    const response = await apiClient.delete<CartDto>(`/api/v1/carts/${customerId}/items/${itemId}`);

    return mapCart(response.data);
  },

  async clearCart(customerId = apiConfig.demoCustomerId): Promise<Cart> {
    const response = await apiClient.delete<CartDto>(`/api/v1/carts/${customerId}/items`);

    return mapCart(response.data);
  },
};
