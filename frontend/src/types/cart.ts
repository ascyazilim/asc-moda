import { Product } from './product';

export type CartItem = {
  id: string;
  product: Product;
  color: string;
  size: string;
  quantity: number;
};

export type CartTotals = {
  subtotal: number;
  discount: number;
  shipping: number;
  total: number;
};

