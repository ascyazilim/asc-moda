const ACCESS_TOKEN_STORAGE_KEY = 'asc_moda_access_token';
const UNAUTHORIZED_EVENT = 'asc_moda_auth_unauthorized';

type AccessTokenProvider = () => Promise<string | undefined> | string | undefined;

let accessTokenProvider: AccessTokenProvider | undefined;

export function setAccessTokenProvider(provider: AccessTokenProvider | undefined) {
  accessTokenProvider = provider;
}

export async function resolveAccessToken() {
  if (accessTokenProvider) {
    const token = await accessTokenProvider();

    if (token) {
      return token;
    }
  }

  return getStoredAccessToken();
}

export function persistAccessToken(token: string | undefined) {
  if (typeof window === 'undefined') {
    return;
  }

  if (token) {
    window.localStorage.setItem(ACCESS_TOKEN_STORAGE_KEY, token);
    return;
  }

  window.localStorage.removeItem(ACCESS_TOKEN_STORAGE_KEY);
}

export function getStoredAccessToken() {
  if (typeof window === 'undefined') {
    return undefined;
  }

  return window.localStorage.getItem(ACCESS_TOKEN_STORAGE_KEY) ?? undefined;
}

export function clearAccessToken() {
  persistAccessToken(undefined);
}

export function emitUnauthorizedSession() {
  if (typeof window !== 'undefined') {
    window.dispatchEvent(new Event(UNAUTHORIZED_EVENT));
  }
}

export function subscribeUnauthorizedSession(listener: () => void) {
  if (typeof window === 'undefined') {
    return () => undefined;
  }

  window.addEventListener(UNAUTHORIZED_EVENT, listener);

  return () => window.removeEventListener(UNAUTHORIZED_EVENT, listener);
}
