export const queryKeys = {
  products: ['products'] as const,
  featuredProducts: ['products', 'featured'] as const,
  newArrivals: ['products', 'new-arrivals'] as const,
  productDetail: (slug: string) => ['products', slug] as const,
  search: (query: string) => ['search', query] as const,
};

