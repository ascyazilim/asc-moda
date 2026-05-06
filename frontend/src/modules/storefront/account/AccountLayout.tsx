import LocationOnOutlinedIcon from '@mui/icons-material/LocationOnOutlined';
import PersonOutlineOutlinedIcon from '@mui/icons-material/PersonOutlineOutlined';
import ReceiptLongOutlinedIcon from '@mui/icons-material/ReceiptLongOutlined';
import SpaceDashboardOutlinedIcon from '@mui/icons-material/SpaceDashboardOutlined';
import { Box, Button, Chip, Divider, Paper, Stack, Tab, Tabs, Typography } from '@mui/material';
import { Link as RouterLink, Outlet, useLocation } from 'react-router-dom';

import { useAuth } from '../../../app/auth/AuthContext';
import { PageContainer } from '../../../components/common/PageContainer';
import { useCurrentCustomer } from '../../../hooks/useAccountQueries';

const accountNavItems = [
  {
    label: 'Özet',
    path: '/account',
    icon: <SpaceDashboardOutlinedIcon fontSize="small" />,
  },
  {
    label: 'Profil',
    path: '/account/profile',
    icon: <PersonOutlineOutlinedIcon fontSize="small" />,
  },
  {
    label: 'Adreslerim',
    path: '/account/addresses',
    icon: <LocationOnOutlinedIcon fontSize="small" />,
  },
  {
    label: 'Siparişlerim',
    path: '/account/orders',
    icon: <ReceiptLongOutlinedIcon fontSize="small" />,
  },
];

export function AccountLayout() {
  const auth = useAuth();
  const currentCustomerQuery = useCurrentCustomer();
  const location = useLocation();
  const activePath = getActivePath(location.pathname);
  const customer = currentCustomerQuery.data;

  return (
    <PageContainer>
      <Stack spacing={{ xs: 3, md: 4 }}>
        <Box
          sx={{
            display: 'grid',
            gridTemplateColumns: { xs: '1fr', md: 'minmax(0, 1fr) auto' },
            gap: 2,
            alignItems: 'end',
          }}
        >
          <Stack spacing={1}>
            <Typography variant="overline" color="secondary.dark" sx={{ fontWeight: 800 }}>
              Hesabım
            </Typography>
            <Typography variant="h2">
              {customer?.displayName ?? auth.currentUser?.displayName ?? 'Hesap alanı'}
            </Typography>
            <Typography color="text.secondary" sx={{ maxWidth: 760 }}>
              Profil, adres ve sipariş bilgilerinizi tek yerden yönetin.
            </Typography>
          </Stack>
          {customer ? (
            <Chip
              label={customer.status}
              color={customer.status === 'ACTIVE' ? 'success' : 'default'}
              variant="outlined"
              sx={{ justifySelf: { md: 'end' } }}
            />
          ) : null}
        </Box>

        <Tabs
          value={activePath}
          variant="scrollable"
          scrollButtons="auto"
          sx={{ display: { xs: 'flex', md: 'none' }, borderBottom: 1, borderColor: 'divider' }}
        >
          {accountNavItems.map((item) => (
            <Tab
              key={item.path}
              value={item.path}
              label={item.label}
              component={RouterLink}
              to={item.path}
              icon={item.icon}
              iconPosition="start"
            />
          ))}
        </Tabs>

        <Box
          sx={{
            display: 'grid',
            gridTemplateColumns: { xs: '1fr', md: '260px minmax(0, 1fr)' },
            gap: { xs: 3, md: 4 },
            alignItems: 'start',
          }}
        >
          <Paper
            elevation={0}
            sx={{
              display: { xs: 'none', md: 'block' },
              border: 1,
              borderColor: 'divider',
              p: 1,
              position: 'sticky',
              top: 104,
            }}
          >
            <Stack spacing={0.5}>
              {accountNavItems.map((item) => (
                <Button
                  key={item.path}
                  component={RouterLink}
                  to={item.path}
                  startIcon={item.icon}
                  color={activePath === item.path ? 'primary' : 'inherit'}
                  variant={activePath === item.path ? 'contained' : 'text'}
                  sx={{ justifyContent: 'flex-start' }}
                >
                  {item.label}
                </Button>
              ))}
              <Divider sx={{ my: 1 }} />
              <Button color="inherit" onClick={() => void auth.logout()} sx={{ justifyContent: 'flex-start' }}>
                Çıkış Yap
              </Button>
            </Stack>
          </Paper>

          <Box sx={{ minWidth: 0 }}>
            <Outlet />
          </Box>
        </Box>
      </Stack>
    </PageContainer>
  );
}

function getActivePath(pathname: string) {
  if (pathname.startsWith('/account/orders')) {
    return '/account/orders';
  }

  if (pathname.startsWith('/account/addresses')) {
    return '/account/addresses';
  }

  if (pathname.startsWith('/account/profile')) {
    return '/account/profile';
  }

  return '/account';
}
