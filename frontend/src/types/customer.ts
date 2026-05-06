export type CustomerStatus = 'ACTIVE' | 'BLOCKED' | 'DELETED' | string;

export type AddressType = 'SHIPPING' | 'BILLING' | 'OTHER';

export type CustomerProfile = {
  id: string;
  externalUserId?: string;
  email: string;
  phoneNumber?: string;
  firstName: string;
  lastName: string;
  fullName: string;
  displayName: string;
  status: CustomerStatus;
  emailVerified: boolean;
  phoneVerified: boolean;
  marketingConsent: boolean;
  addresses: CustomerAddress[];
  createdAt?: string;
  updatedAt?: string;
};

export type CustomerAddress = {
  id: string;
  customerId: string;
  title: string;
  addressType: AddressType;
  fullName: string;
  phoneNumber: string;
  city: string;
  district: string;
  addressLine: string;
  postalCode?: string;
  country: string;
  defaultShipping: boolean;
  defaultBilling: boolean;
  active: boolean;
  createdAt?: string;
  updatedAt?: string;
};

export type UpdateCustomerProfileInput = {
  email?: string;
  phoneNumber?: string;
  firstName?: string;
  lastName?: string;
  marketingConsent?: boolean;
};

export type CustomerAddressInput = {
  title: string;
  addressType: AddressType;
  fullName: string;
  phoneNumber: string;
  city: string;
  district: string;
  addressLine: string;
  postalCode?: string;
  country: string;
  defaultShipping: boolean;
  defaultBilling: boolean;
};
