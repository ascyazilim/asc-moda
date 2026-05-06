import { CurrentUser } from '../../app/auth/authTypes';
import {
  AddressType,
  CustomerAddress,
  CustomerAddressInput,
  CustomerProfile,
  UpdateCustomerProfileInput,
} from '../../types/customer';
import { apiClient } from './client';

type CustomerResponseDto = {
  id: string;
  externalUserId?: string | null;
  email: string;
  phoneNumber?: string | null;
  firstName: string;
  lastName: string;
  fullName: string;
  status: string;
  emailVerified: boolean;
  phoneVerified: boolean;
  marketingConsent: boolean;
  addresses?: CustomerAddressResponseDto[];
  createdAt?: string;
  updatedAt?: string;
};

type CustomerAddressResponseDto = {
  id: string;
  customerId: string;
  title: string;
  addressType: AddressType;
  fullName: string;
  phoneNumber: string;
  city: string;
  district: string;
  addressLine: string;
  postalCode?: string | null;
  country: string;
  defaultShipping: boolean;
  defaultBilling: boolean;
  active: boolean;
  createdAt?: string;
  updatedAt?: string;
};

type CreateCustomerRequestDto = {
  externalUserId: string;
  email: string;
  phoneNumber?: string;
  firstName: string;
  lastName: string;
  emailVerified: boolean;
  phoneVerified: boolean;
  marketingConsent: boolean;
};

type UpdateCustomerProfileRequestDto = UpdateCustomerProfileInput & {
  externalUserId?: string;
};

export const customerApi = {
  async ensureCurrentCustomer(user: CurrentUser): Promise<CustomerProfile> {
    const response = await apiClient.post<CustomerResponseDto>('/api/v1/customers', {
      externalUserId: user.sub,
      email: requireEmail(user),
      phoneNumber: user.phoneNumber,
      ...resolveNames(user),
      emailVerified: user.emailVerified,
      phoneVerified: user.phoneVerified,
      marketingConsent: false,
    } satisfies CreateCustomerRequestDto);

    return mapCustomer(response.data);
  },

  async getCustomer(customerId: string): Promise<CustomerProfile> {
    const response = await apiClient.get<CustomerResponseDto>(`/api/v1/customers/${customerId}`);

    return mapCustomer(response.data);
  },

  async updateProfile(
    customerId: string,
    payload: UpdateCustomerProfileRequestDto,
  ): Promise<CustomerProfile> {
    const response = await apiClient.patch<CustomerResponseDto>(
      `/api/v1/customers/${customerId}`,
      payload,
    );

    return mapCustomer(response.data);
  },

  async listAddresses(customerId: string): Promise<CustomerAddress[]> {
    const response = await apiClient.get<CustomerAddressResponseDto[]>(
      `/api/v1/customers/${customerId}/addresses`,
    );

    return response.data.map(mapAddress);
  },

  async addAddress(customerId: string, payload: CustomerAddressInput): Promise<CustomerAddress> {
    const response = await apiClient.post<CustomerAddressResponseDto>(
      `/api/v1/customers/${customerId}/addresses`,
      normalizeAddressPayload(payload),
    );

    return mapAddress(response.data);
  },

  async updateAddress(
    customerId: string,
    addressId: string,
    payload: CustomerAddressInput,
  ): Promise<CustomerAddress> {
    const response = await apiClient.patch<CustomerAddressResponseDto>(
      `/api/v1/customers/${customerId}/addresses/${addressId}`,
      normalizeAddressPayload(payload),
    );

    return mapAddress(response.data);
  },

  async deactivateAddress(customerId: string, addressId: string): Promise<CustomerAddress> {
    const response = await apiClient.delete<CustomerAddressResponseDto>(
      `/api/v1/customers/${customerId}/addresses/${addressId}`,
    );

    return mapAddress(response.data);
  },

  async setDefaultShipping(customerId: string, addressId: string): Promise<CustomerAddress> {
    const response = await apiClient.patch<CustomerAddressResponseDto>(
      `/api/v1/customers/${customerId}/addresses/${addressId}/default-shipping`,
    );

    return mapAddress(response.data);
  },

  async setDefaultBilling(customerId: string, addressId: string): Promise<CustomerAddress> {
    const response = await apiClient.patch<CustomerAddressResponseDto>(
      `/api/v1/customers/${customerId}/addresses/${addressId}/default-billing`,
    );

    return mapAddress(response.data);
  },
};

function mapCustomer(dto: CustomerResponseDto): CustomerProfile {
  const fullName = dto.fullName || [dto.firstName, dto.lastName].filter(Boolean).join(' ');

  return {
    id: dto.id,
    externalUserId: dto.externalUserId ?? undefined,
    email: dto.email,
    phoneNumber: dto.phoneNumber ?? undefined,
    firstName: dto.firstName,
    lastName: dto.lastName,
    fullName,
    displayName: fullName || dto.email,
    status: dto.status,
    emailVerified: dto.emailVerified,
    phoneVerified: dto.phoneVerified,
    marketingConsent: dto.marketingConsent,
    addresses: (dto.addresses ?? []).map(mapAddress),
    createdAt: dto.createdAt,
    updatedAt: dto.updatedAt,
  };
}

function mapAddress(dto: CustomerAddressResponseDto): CustomerAddress {
  return {
    id: dto.id,
    customerId: dto.customerId,
    title: dto.title,
    addressType: dto.addressType,
    fullName: dto.fullName,
    phoneNumber: dto.phoneNumber,
    city: dto.city,
    district: dto.district,
    addressLine: dto.addressLine,
    postalCode: dto.postalCode ?? undefined,
    country: dto.country,
    defaultShipping: dto.defaultShipping,
    defaultBilling: dto.defaultBilling,
    active: dto.active,
    createdAt: dto.createdAt,
    updatedAt: dto.updatedAt,
  };
}

function normalizeAddressPayload(payload: CustomerAddressInput) {
  return {
    ...payload,
    postalCode: payload.postalCode?.trim() || undefined,
  };
}

function requireEmail(user: CurrentUser) {
  if (!user.email) {
    throw new Error('Keycloak e-posta claimi eksik. Musteri profili olusturulamadi.');
  }

  return user.email;
}

function resolveNames(user: CurrentUser) {
  if (user.firstName && user.lastName) {
    return {
      firstName: user.firstName,
      lastName: user.lastName,
    };
  }

  const parts = (user.fullName || user.displayName || user.preferredUsername || user.sub)
    .trim()
    .split(/\s+/);
  const firstName = user.firstName || parts[0] || user.preferredUsername || user.sub;
  const lastName = user.lastName || parts.slice(1).join(' ') || '-';

  return {
    firstName,
    lastName,
  };
}
