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
};
