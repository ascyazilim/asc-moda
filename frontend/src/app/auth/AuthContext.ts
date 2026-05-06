import { createContext, useContext } from 'react';

import { AuthStatus, CurrentUser } from './authTypes';

export type AuthContextValue = {
  status: AuthStatus;
  isConfigured: boolean;
  isLoading: boolean;
  isAuthenticated: boolean;
  currentUser: CurrentUser | null;
  error?: string;
  login: (redirectTo?: string) => Promise<void>;
  logout: () => Promise<void>;
  refreshToken: (minValidity?: number) => Promise<string | undefined>;
};

export const AuthContext = createContext<AuthContextValue | undefined>(undefined);

export function useAuth() {
  const context = useContext(AuthContext);

  if (!context) {
    throw new Error('useAuth must be used inside AuthProvider.');
  }

  return context;
}
