import { KeycloakTokenParsed } from 'keycloak-js';

import { CurrentUser } from './authTypes';

type AscModaToken = KeycloakTokenParsed & {
  preferred_username?: string;
  email?: string;
  given_name?: string;
  family_name?: string;
  name?: string;
  phone_number?: string;
  email_verified?: boolean;
  phone_number_verified?: boolean;
  realm_access?: {
    roles?: string[];
  };
  resource_access?: Record<
    string,
    {
      roles?: string[];
    }
  >;
};

export function buildCurrentUser(token: KeycloakTokenParsed | undefined): CurrentUser | null {
  if (!token?.sub) {
    return null;
  }

  const parsed = token as AscModaToken;
  const subject = token.sub;
  const roles = collectRoles(parsed);
  const fullName = parsed.name?.trim() || [parsed.given_name, parsed.family_name].filter(Boolean).join(' ');
  const displayName =
    fullName ||
    parsed.preferred_username ||
    parsed.email ||
    subject;

  return {
    sub: subject,
    username: parsed.preferred_username,
    preferredUsername: parsed.preferred_username,
    email: parsed.email,
    firstName: parsed.given_name,
    lastName: parsed.family_name,
    fullName: fullName || undefined,
    displayName,
    phoneNumber: parsed.phone_number,
    roles,
    authenticated: true,
    emailVerified: Boolean(parsed.email_verified),
    phoneVerified: Boolean(parsed.phone_number_verified),
    isCustomer: hasRole(roles, 'ROLE_CUSTOMER', 'customer'),
    isAdmin: hasRole(roles, 'ROLE_ADMIN', 'admin'),
  };
}

function collectRoles(token: AscModaToken) {
  const roles = new Set<string>();

  token.realm_access?.roles?.forEach((role) => roles.add(role));

  Object.values(token.resource_access ?? {}).forEach((access) => {
    access.roles?.forEach((role) => roles.add(role));
  });

  return Array.from(roles);
}

function hasRole(roles: string[], authority: string, keycloakRole: string) {
  return roles.some((role) => role === authority || role.toLowerCase() === keycloakRole);
}
