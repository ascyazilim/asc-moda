export const apiConfig = {
  baseUrl: import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080',
  demoCustomerId:
    import.meta.env.VITE_DEMO_CUSTOMER_ID ?? '00000000-0000-0000-0000-000000000001',
  keycloak: {
    url: import.meta.env.VITE_KEYCLOAK_URL ?? '',
    realm: import.meta.env.VITE_KEYCLOAK_REALM ?? '',
    clientId: import.meta.env.VITE_KEYCLOAK_CLIENT_ID ?? '',
  },
};
