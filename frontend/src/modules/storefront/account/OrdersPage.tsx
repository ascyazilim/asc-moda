import ArrowForwardIcon from '@mui/icons-material/ArrowForward';
import Inventory2OutlinedIcon from '@mui/icons-material/Inventory2Outlined';
import {
  Alert,
  Box,
  Button,
  Chip,
  CircularProgress,
  Pagination,
  Paper,
  Stack,
  Typography,
} from '@mui/material';
import { useState } from 'react';
import { Link as RouterLink } from 'react-router-dom';

import { EmptyState } from '../../../components/common/EmptyState';
import { useCurrentCustomer, useCustomerOrders } from '../../../hooks/useAccountQueries';
import { OrderSummary } from '../../../types/order';
import { formatCurrency, formatDate } from '../../../utils/formatters';

export function OrdersPage() {
  const [page, setPage] = useState(1);
  const currentCustomerQuery = useCurrentCustomer();
  const ordersQuery = useCustomerOrders(page);
  const orders = ordersQuery.data?.items ?? [];
  const isLoading = currentCustomerQuery.isLoading || ordersQuery.isLoading;

  return (
    <Stack spacing={3}>
      <Paper elevation={0} sx={{ border: 1, borderColor: 'divider', p: { xs: 2.5, md: 3 } }}>
        <Typography variant="h4">Siparişlerim</Typography>
        <Typography color="text.secondary">
          Sipariş durumunuzu, toplamlarınızı ve detay satırlarınızı buradan takip edin.
        </Typography>
      </Paper>

      {isLoading ? (
        <Stack alignItems="center" sx={{ py: 8 }}>
          <CircularProgress color="secondary" />
        </Stack>
      ) : null}

      {ordersQuery.isError || currentCustomerQuery.isError ? (
        <Alert severity="error">Siparişleriniz şu anda yüklenemiyor.</Alert>
      ) : null}

      {!isLoading && !orders.length && !ordersQuery.isError ? (
        <EmptyState
          title="Henüz sipariş yok"
          description="Sipariş oluşturduğunuzda burada listelenecek."
          actionLabel="Koleksiyonu Keşfet"
          actionHref="/products"
        />
      ) : null}

      {!isLoading && orders.length ? (
        <Stack spacing={1.5}>
          {orders.map((order) => (
            <OrderListCard key={order.id} order={order} />
          ))}
        </Stack>
      ) : null}

      {ordersQuery.data && ordersQuery.data.totalPages > 1 ? (
        <Stack alignItems="center">
          <Pagination
            count={ordersQuery.data.totalPages}
            page={page}
            onChange={(_, value) => setPage(value)}
            color="primary"
          />
        </Stack>
      ) : null}
    </Stack>
  );
}

function OrderListCard({ order }: { order: OrderSummary }) {
  return (
    <Paper elevation={0} sx={{ border: 1, borderColor: 'divider', p: { xs: 2, md: 2.5 } }}>
      <Box
        sx={{
          display: 'grid',
          gridTemplateColumns: { xs: '1fr', md: '1.2fr 0.8fr 0.8fr auto' },
          gap: { xs: 1.5, md: 2 },
          alignItems: 'center',
        }}
      >
        <Stack direction="row" spacing={1.5} alignItems="center" sx={{ minWidth: 0 }}>
          <Box
            sx={{
              width: 44,
              height: 44,
              borderRadius: 1,
              display: 'grid',
              placeItems: 'center',
              bgcolor: 'secondary.light',
              color: 'primary.dark',
              flex: '0 0 auto',
            }}
          >
            <Inventory2OutlinedIcon />
          </Box>
          <Box sx={{ minWidth: 0 }}>
            <Typography variant="h5" noWrap>
              {order.orderNumber}
            </Typography>
            <Typography variant="body2" color="text.secondary">
              {formatDate(order.placedAt)} · {order.itemCount} ürün
            </Typography>
          </Box>
        </Stack>

        <StatusChip status={order.status} />
        <Typography fontWeight={800}>{formatCurrency(order.totalAmount)}</Typography>
        <Button
          component={RouterLink}
          to={`/account/orders/${order.id}`}
          endIcon={<ArrowForwardIcon />}
          variant="outlined"
        >
          Detay
        </Button>
      </Box>
    </Paper>
  );
}

function StatusChip({ status }: { status: string }) {
  const normalized = status.toUpperCase();
  const color = normalized === 'CONFIRMED' ? 'success' : normalized === 'CANCELLED' ? 'default' : 'secondary';

  return <Chip label={statusLabel(normalized)} color={color} variant="outlined" sx={{ justifySelf: 'start' }} />;
}

function statusLabel(status: string) {
  if (status === 'CONFIRMED') {
    return 'Onaylandı';
  }

  if (status === 'CANCELLED') {
    return 'İptal';
  }

  if (status === 'FAILED') {
    return 'Başarısız';
  }

  return 'Beklemede';
}
