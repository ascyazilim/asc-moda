export type CurrentUser = {
  sub: string;
  username?: string;
  preferredUsername?: string;
  email?: string;
  firstName?: string;
  lastName?: string;
  fullName?: string;
  displayName: string;
  phoneNumber?: string;
  roles: string[];
  authenticated: boolean;
  emailVerified: boolean;
  phoneVerified: boolean;
  isCustomer: boolean;
  isAdmin: boolean;
};

export type AuthStatus = 'loading' | 'authenticated' | 'unauthenticated' | 'error';
