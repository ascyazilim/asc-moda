import ArrowForwardIcon from '@mui/icons-material/ArrowForward';
import LocationOnOutlinedIcon from '@mui/icons-material/LocationOnOutlined';
import PersonOutlineOutlinedIcon from '@mui/icons-material/PersonOutlineOutlined';
import ReceiptLongOutlinedIcon from '@mui/icons-material/ReceiptLongOutlined';
import { Alert, Box, Button, CircularProgress, Paper, Stack, Typography } from '@mui/material';
import { ReactNode } from 'react';
import { Link as RouterLink } from 'react-router-dom';

import { useCurrentCustomer, useCustomerAddresses, useCustomerOrders } from '../../../hooks/useAccountQueries';
import { formatCurrency, formatDate } from '../../../utils/formatters';

export function AccountOverviewPage() {
  const currentCustomerQuery = useCurrentCustomer();
  const addressesQuery = useCustomerAddresses();
  const ordersQuery = useCustomerOrders(1);
  const customer = currentCustomerQuery.data;
  const latestOrder = ordersQuery.data?.items[0];

  if (currentCustomerQuery.isLoading) {
    return (
      <Stack alignItems="center" sx={{ py: 8 }}>
        <CircularProgress color="secondary" />
      </Stack>
    );
  }

  if (currentCustomerQuery.isError) {
    return (
      <Alert severity="error">
        Hesap bilgileriniz hazırlanamadı. Keycloak oturumunuzun müşteri yetkisiyle geldiğini kontrol edin.
      </Alert>
    );
  }

  return (
    <Stack spacing={3}>
      <Paper elevation={0} sx={{ border: 1, borderColor: 'divider', p: { xs: 2.5, md: 3 } }}>
        <Stack spacing={1}>
          <Typography variant="h4">Hoş geldiniz, {customer?.displayName}</Typography>
          <Typography color="text.secondary">
            E-posta doğrulama, varsayılan adresler ve son siparişleriniz burada özetlenir.
          </Typography>
        </Stack>
      </Paper>

      <Box
        sx={{
          display: 'grid',
          gridTemplateColumns: { xs: '1fr', lg: 'repeat(3, minmax(0, 1fr))' },
          gap: 2,
        }}
      >
        <OverviewCard
          icon={<PersonOutlineOutlinedIcon />}
          title="Profil"
          description={customer?.email ?? 'Profil bilgilerinizi tamamlayın.'}
          meta={customer?.emailVerified ? 'E-posta doğrulandı' : 'E-posta doğrulaması bekliyor'}
          href="/account/profile"
        />
        <OverviewCard
          icon={<LocationOnOutlinedIcon />}
          title="Adresler"
          description={`${addressesQuery.data?.filter((address) => address.active).length ?? 0} kayıtlı adres`}
          meta={customer?.addresses.some((address) => address.defaultShipping) ? 'Varsayılan teslimat hazır' : 'Teslimat adresi seçin'}
          href="/account/addresses"
        />
        <OverviewCard
          icon={<ReceiptLongOutlinedIcon />}
          title="Siparişler"
          description={
            latestOrder
              ? `${latestOrder.orderNumber} - ${formatCurrency(latestOrder.totalAmount)}`
              : 'Henüz siparişiniz yok'
          }
          meta={latestOrder ? formatDate(latestOrder.placedAt) : 'Koleksiyonu keşfetmeye devam edin'}
          href="/account/orders"
        />
      </Box>
    </Stack>
  );
}

type OverviewCardProps = {
  icon: ReactNode;
  title: string;
  description: string;
  meta: string;
  href: string;
};

function OverviewCard({ icon, title, description, meta, href }: OverviewCardProps) {
  return (
    <Paper elevation={0} sx={{ border: 1, borderColor: 'divider', p: 2.5, height: '100%' }}>
      <Stack spacing={2} sx={{ height: '100%' }}>
        <Box sx={{ color: 'secondary.dark' }}>{icon}</Box>
        <Stack spacing={0.75} sx={{ flex: 1 }}>
          <Typography variant="h5">{title}</Typography>
          <Typography color="text.secondary">{description}</Typography>
          <Typography variant="body2" color="text.secondary">
            {meta}
          </Typography>
        </Stack>
        <Button component={RouterLink} to={href} endIcon={<ArrowForwardIcon />} sx={{ alignSelf: 'flex-start' }}>
          Görüntüle
        </Button>
      </Stack>
    </Paper>
  );
}
