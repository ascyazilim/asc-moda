import axios, { AxiosError } from 'axios';

import { apiConfig } from './config';

export const apiClient = axios.create({
  baseURL: apiConfig.baseUrl,
  timeout: 15000,
  headers: {
    'Content-Type': 'application/json',
  },
});

apiClient.interceptors.request.use((config) => {
  const token = window.localStorage.getItem('asc_moda_access_token');

  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }

  return config;
});

export type ApiError = {
  message: string;
  status?: number;
};

export function normalizeApiError(error: unknown): ApiError {
  if (error instanceof AxiosError) {
    return {
      message:
        error.response?.data?.message ??
        error.response?.data?.detail ??
        error.response?.data?.title ??
        error.message ??
        'Beklenmeyen bir servis hatası oluştu.',
      status: error.response?.status,
    };
  }

  if (error instanceof Error) {
    return {
      message: error.message,
    };
  }

  return {
    message: 'Beklenmeyen bir hata oluştu.',
  };
}
