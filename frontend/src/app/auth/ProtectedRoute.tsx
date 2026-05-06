import { Box, CircularProgress, Stack, Typography } from '@mui/material';
import { useEffect, useRef } from 'react';
import { Outlet, useLocation } from 'react-router-dom';

import { EmptyState } from '../../components/common/EmptyState';
import { PageContainer } from '../../components/common/PageContainer';
import { useAuth } from './AuthContext';

export function ProtectedRoute() {
  const auth = useAuth();
  const location = useLocation();
  const loginStartedRef = useRef(false);

  useEffect(() => {
    if (
      !auth.isLoading &&
      !auth.isAuthenticated &&
      auth.isConfigured &&
      auth.status !== 'error' &&
      !loginStartedRef.current
    ) {
      loginStartedRef.current = true;
      void auth.login(`${location.pathname}${location.search}`);
    }
  }, [auth, location.pathname, location.search]);

  if (auth.isLoading) {
    return <ProtectedRouteLoader label="Oturum kontrol ediliyor" />;
  }

  if (!auth.isConfigured) {
    return (
      <PageContainer>
        <EmptyState
          title="Giris ayarlari eksik"
          description="Hesap alanlarini kullanmak icin Keycloak ortam degiskenlerinin tanimli olmasi gerekiyor."
        />
      </PageContainer>
    );
  }

  if (auth.status === 'error') {
    return (
      <PageContainer>
        <EmptyState
          title="Oturum baslatilamadi"
          description={auth.error ?? 'Keycloak oturumu su anda hazir degil.'}
        />
      </PageContainer>
    );
  }

  if (!auth.isAuthenticated) {
    return <ProtectedRouteLoader label="Giris sayfasina yonlendiriliyorsunuz" />;
  }

  return <Outlet />;
}

function ProtectedRouteLoader({ label }: { label: string }) {
  return (
    <PageContainer>
      <Box sx={{ minHeight: 360, display: 'grid', placeItems: 'center' }}>
        <Stack alignItems="center" spacing={2}>
          <CircularProgress color="secondary" />
          <Typography color="text.secondary">{label}</Typography>
        </Stack>
      </Box>
    </PageContainer>
  );
}
