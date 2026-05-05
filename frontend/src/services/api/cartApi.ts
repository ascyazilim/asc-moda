import { CartItem } from '../../types/cart';
import { apiClient } from './client';

export type AddCartItemPayload = {
  productId: string;
  color: string;
  size: string;
  quantity: number;
};

export const cartApi = {
  async getCart(): Promise<CartItem[]> {
    const response = await apiClient.get<CartItem[]>('/cart');

    return response.data;
  },

  async addItem(payload: AddCartItemPayload): Promise<CartItem[]> {
    const response = await apiClient.post<CartItem[]>('/cart/items', payload);

    return response.data;
  },

  async updateQuantity(itemId: string, quantity: number): Promise<CartItem[]> {
    const response = await apiClient.patch<CartItem[]>(`/cart/items/${itemId}`, {
      quantity,
    });

    return response.data;
  },

  async removeItem(itemId: string): Promise<CartItem[]> {
    const response = await apiClient.delete<CartItem[]>(`/cart/items/${itemId}`);

    return response.data;
  },
};

