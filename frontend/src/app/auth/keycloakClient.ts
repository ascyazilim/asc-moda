import Keycloak, { KeycloakInitOptions, KeycloakInstance } from 'keycloak-js';

import { apiConfig } from '../../services/api/config';

let keycloakClient: KeycloakInstance | undefined;
let keycloakInitPromise: Promise<boolean> | undefined;

export function isKeycloakConfigured() {
  return Boolean(
    apiConfig.keycloak.url &&
      apiConfig.keycloak.realm &&
      apiConfig.keycloak.clientId,
  );
}

export function getKeycloakClient() {
  if (!isKeycloakConfigured()) {
    return undefined;
  }

  if (!keycloakClient) {
    keycloakClient = new Keycloak({
      url: apiConfig.keycloak.url,
      realm: apiConfig.keycloak.realm,
      clientId: apiConfig.keycloak.clientId,
    });
  }

  return keycloakClient;
}

export function initKeycloakClient(options: KeycloakInitOptions) {
  const client = getKeycloakClient();

  if (!client) {
    return Promise.resolve(false);
  }

  if (!keycloakInitPromise) {
    keycloakInitPromise = client.init(options).catch((error: unknown) => {
      keycloakInitPromise = undefined;
      throw error;
    });
  }

  return keycloakInitPromise;
}
