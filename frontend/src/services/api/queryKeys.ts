export const queryKeys = {
  products: ['products'] as const,
  productList: (filters: unknown) => ['products', 'list', filters] as const,
  featuredProducts: ['products', 'featured'] as const,
  newArrivals: ['products', 'new-arrivals'] as const,
  productDetail: (slug: string) => ['products', slug] as const,
  categories: ['categories'] as const,
  search: (query: string, filters: unknown) => ['search', query, filters] as const,
  cart: (customerId: string) => ['cart', customerId] as const,
  cartSummary: (customerId: string) => ['cart', customerId, 'summary'] as const,
  currentCustomer: (subject: string | undefined) => ['customer', 'current', subject ?? 'guest'] as const,
  customerAddresses: (customerId: string | undefined) =>
    ['customer', customerId ?? 'pending', 'addresses'] as const,
  customerOrders: (customerId: string | undefined, page: number) =>
    ['customer', customerId ?? 'pending', 'orders', page] as const,
  orderDetail: (orderId: string | undefined) => ['orders', orderId ?? 'pending'] as const,
};
