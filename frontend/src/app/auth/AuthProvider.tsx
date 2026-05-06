import { PropsWithChildren, useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { KeycloakInstance } from 'keycloak-js';

import { AuthContext } from './AuthContext';
import {
  clearAccessToken,
  persistAccessToken,
  setAccessTokenProvider,
  subscribeUnauthorizedSession,
} from './authSession';
import { AuthStatus, CurrentUser } from './authTypes';
import { buildCurrentUser } from './authUser';
import { getKeycloakClient, initKeycloakClient, isKeycloakConfigured } from './keycloakClient';

export function AuthProvider({ children }: PropsWithChildren) {
  const configured = isKeycloakConfigured();
  const keycloakRef = useRef<KeycloakInstance>();
  const [status, setStatus] = useState<AuthStatus>('loading');
  const [currentUser, setCurrentUser] = useState<CurrentUser | null>(null);
  const [error, setError] = useState<string>();

  const expireSession = useCallback(() => {
    keycloakRef.current?.clearToken();
    clearAccessToken();
    setCurrentUser(null);
    setStatus(configured ? 'unauthenticated' : 'unauthenticated');
  }, [configured]);

  const syncClientState = useCallback((client: KeycloakInstance, authenticated: boolean) => {
    if (!authenticated) {
      clearAccessToken();
      setCurrentUser(null);
      setStatus('unauthenticated');
      return;
    }

    const user = buildCurrentUser(client.tokenParsed);

    if (!user || !client.token) {
      clearAccessToken();
      setCurrentUser(null);
      setStatus('unauthenticated');
      return;
    }

    persistAccessToken(client.token);
    setCurrentUser(user);
    setError(undefined);
    setStatus('authenticated');
  }, []);

  const refreshToken = useCallback(
    async (minValidity = 30) => {
      const client = keycloakRef.current;

      if (!client?.authenticated) {
        return undefined;
      }

      try {
        await client.updateToken(minValidity);
        syncClientState(client, true);
        return client.token;
      } catch {
        expireSession();
        return undefined;
      }
    },
    [expireSession, syncClientState],
  );

  useEffect(() => {
    setAccessTokenProvider(refreshToken);

    return () => setAccessTokenProvider(undefined);
  }, [refreshToken]);

  useEffect(() => {
    if (!configured) {
      clearAccessToken();
      setCurrentUser(null);
      setStatus('unauthenticated');
      return;
    }

    const client = getKeycloakClient();

    if (!client) {
      clearAccessToken();
      setStatus('unauthenticated');
      return;
    }

    keycloakRef.current = client;
    let active = true;

    client.onAuthSuccess = () => syncClientState(client, true);
    client.onAuthRefreshSuccess = () => syncClientState(client, true);
    client.onAuthLogout = () => expireSession();
    client.onTokenExpired = () => {
      void refreshToken();
    };

    const unsubscribeUnauthorized = subscribeUnauthorizedSession(expireSession);

    initKeycloakClient({
        onLoad: 'check-sso',
        pkceMethod: 'S256',
        checkLoginIframe: false,
      })
      .then((authenticated) => {
        if (active) {
          syncClientState(client, authenticated);
        }
      })
      .catch(() => {
        if (!active) {
          return;
        }

        clearAccessToken();
        setCurrentUser(null);
        setError('Keycloak oturumu baslatilamadi.');
        setStatus('error');
      });

    return () => {
      active = false;
      unsubscribeUnauthorized();
      client.onAuthSuccess = undefined;
      client.onAuthRefreshSuccess = undefined;
      client.onAuthLogout = undefined;
      client.onTokenExpired = undefined;
    };
  }, [configured, expireSession, refreshToken, syncClientState]);

  const login = useCallback(
    async (redirectTo?: string) => {
      const client = keycloakRef.current ?? getKeycloakClient();

      if (!configured || !client) {
        setError('Keycloak ayarlari eksik.');
        return;
      }

      await client.login({
        redirectUri: toAbsoluteRedirectUri(redirectTo),
      });
    },
    [configured],
  );

  const logout = useCallback(async () => {
    const client = keycloakRef.current;
    clearAccessToken();
    setCurrentUser(null);
    setStatus('unauthenticated');

    if (configured && client?.authenticated) {
      await client.logout({
        redirectUri: window.location.origin,
      });
    }
  }, [configured]);

  const value = useMemo(
    () => ({
      status,
      isConfigured: configured,
      isLoading: status === 'loading',
      isAuthenticated: status === 'authenticated',
      currentUser,
      error,
      login,
      logout,
      refreshToken,
    }),
    [configured, currentUser, error, login, logout, refreshToken, status],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

function toAbsoluteRedirectUri(redirectTo?: string) {
  if (typeof window === 'undefined') {
    return undefined;
  }

  if (!redirectTo) {
    return window.location.href;
  }

  if (redirectTo.startsWith('http://') || redirectTo.startsWith('https://')) {
    return redirectTo;
  }

  return `${window.location.origin}${redirectTo.startsWith('/') ? redirectTo : `/${redirectTo}`}`;
}
