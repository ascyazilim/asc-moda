import ArrowBackIcon from '@mui/icons-material/ArrowBack';
import {
  Alert,
  Box,
  Breadcrumbs,
  Button,
  Chip,
  CircularProgress,
  Divider,
  Link,
  Paper,
  Stack,
  Typography,
} from '@mui/material';
import { Link as RouterLink, useParams } from 'react-router-dom';

import { EmptyState } from '../../../components/common/EmptyState';
import { PriceDisplay } from '../../../components/ui/PriceDisplay';
import { useCustomerOrderDetail } from '../../../hooks/useAccountQueries';
import { OrderAddressSnapshot, OrderDetail, OrderItem } from '../../../types/order';
import { formatCurrency, formatDateTime } from '../../../utils/formatters';

export function OrderDetailPage() {
  const { orderId } = useParams();
  const orderQuery = useCustomerOrderDetail(orderId);
  const order = orderQuery.data;

  if (orderQuery.isLoading) {
    return (
      <Stack alignItems="center" sx={{ py: 8 }}>
        <CircularProgress color="secondary" />
      </Stack>
    );
  }

  if (orderQuery.isError) {
    return (
      <EmptyState
        title="Sipariş yüklenemedi"
        description="Bu siparişe erişim yetkiniz olmayabilir ya da servis şu anda yanıt vermiyor."
        actionLabel="Siparişlere Dön"
        actionHref="/account/orders"
      />
    );
  }

  if (!order) {
    return (
      <EmptyState
        title="Sipariş bulunamadı"
        description="Aradığınız sipariş kaydı bulunamadı."
        actionLabel="Siparişlere Dön"
        actionHref="/account/orders"
      />
    );
  }

  return (
    <Stack spacing={3}>
      <Breadcrumbs sx={{ color: 'text.secondary' }}>
        <Link component={RouterLink} to="/account/orders">
          Siparişlerim
        </Link>
        <Typography color="text.primary">{order.orderNumber}</Typography>
      </Breadcrumbs>

      <Paper elevation={0} sx={{ border: 1, borderColor: 'divider', p: { xs: 2.5, md: 3 } }}>
        <Stack spacing={2}>
          <Button
            component={RouterLink}
            to="/account/orders"
            startIcon={<ArrowBackIcon />}
            color="inherit"
            sx={{ alignSelf: 'flex-start' }}
          >
            Siparişlere Dön
          </Button>
          <Box
            sx={{
              display: 'grid',
              gridTemplateColumns: { xs: '1fr', md: 'minmax(0, 1fr) auto' },
              gap: 2,
              alignItems: 'start',
            }}
          >
            <Box>
              <Typography variant="h3">{order.orderNumber}</Typography>
              <Typography color="text.secondary">
                {formatDateTime(order.placedAt ?? order.createdAt)} tarihinde oluşturuldu.
              </Typography>
            </Box>
            <StatusChip status={order.status} />
          </Box>
          {order.note ? <Alert severity="info">{order.note}</Alert> : null}
          {order.cancellationReason ? (
            <Alert severity="warning">İptal nedeni: {order.cancellationReason}</Alert>
          ) : null}
          {order.failureReason ? <Alert severity="error">Hata nedeni: {order.failureReason}</Alert> : null}
        </Stack>
      </Paper>

      <Box
        sx={{
          display: 'grid',
          gridTemplateColumns: { xs: '1fr', lg: 'minmax(0, 1fr) 340px' },
          gap: 3,
          alignItems: 'start',
        }}
      >
        <Stack spacing={1.5}>
          {order.items.map((item) => (
            <OrderItemRow key={item.id} item={item} />
          ))}
        </Stack>

        <Stack spacing={2}>
          <OrderTotalsCard order={order} />
          {order.shippingAddress ? <AddressSnapshotCard address={order.shippingAddress} /> : null}
        </Stack>
      </Box>
    </Stack>
  );
}

function OrderItemRow({ item }: { item: OrderItem }) {
  return (
    <Paper elevation={0} sx={{ border: 1, borderColor: 'divider', p: { xs: 1.5, sm: 2 } }}>
      <Box
        sx={{
          display: 'grid',
          gridTemplateColumns: { xs: '86px 1fr', md: '104px minmax(0, 1fr) auto' },
          gap: 2,
          alignItems: 'center',
        }}
      >
        <Box
          component="img"
          src={item.imageUrl}
          alt={item.productName}
          sx={{
            width: '100%',
            height: { xs: 112, md: 132 },
            objectFit: 'cover',
            borderRadius: 1,
            bgcolor: 'secondary.light',
          }}
        />
        <Stack spacing={0.75} sx={{ minWidth: 0 }}>
          <Typography component={RouterLink} to={`/products/${item.productSlug}`} variant="h5">
            {item.productName}
          </Typography>
          <Typography variant="body2" color="text.secondary">
            {[item.color, item.size].filter(Boolean).join(' / ') || item.sku}
          </Typography>
          <Typography variant="body2" color="text.secondary">
            {item.quantity} adet · {formatCurrency(item.unitPrice)}
          </Typography>
        </Stack>
        <Stack alignItems={{ xs: 'flex-start', md: 'flex-end' }} sx={{ gridColumn: { xs: '2', md: 'auto' } }}>
          <PriceDisplay price={item.lineTotal} size="sm" />
          {item.reservationStatus ? (
            <Typography variant="body2" color="text.secondary">
              Rezervasyon: {item.reservationStatus}
            </Typography>
          ) : null}
        </Stack>
      </Box>
    </Paper>
  );
}

function OrderTotalsCard({ order }: { order: OrderDetail }) {
  return (
    <Paper elevation={0} sx={{ border: 1, borderColor: 'divider', p: 2.5 }}>
      <Stack spacing={1.5}>
        <Typography variant="h5">Toplamlar</Typography>
        <TotalRow label="Ara toplam" value={formatCurrency(order.subtotalAmount)} />
        {order.discountAmount > 0 ? (
          <TotalRow label="İndirim" value={`-${formatCurrency(order.discountAmount)}`} />
        ) : null}
        <TotalRow label="Kargo" value={formatCurrency(order.shippingAmount)} />
        <Divider />
        <TotalRow label="Genel toplam" value={formatCurrency(order.totalAmount)} strong />
      </Stack>
    </Paper>
  );
}

function AddressSnapshotCard({ address }: { address: OrderAddressSnapshot }) {
  return (
    <Paper elevation={0} sx={{ border: 1, borderColor: 'divider', p: 2.5 }}>
      <Stack spacing={1}>
        <Typography variant="h5">Teslimat Adresi</Typography>
        <Typography fontWeight={700}>{address.fullName}</Typography>
        <Typography color="text.secondary">{address.phoneNumber}</Typography>
        <Typography color="text.secondary">
          {address.addressLine}, {address.district} / {address.city}
        </Typography>
        <Typography color="text.secondary">
          {[address.postalCode, address.country].filter(Boolean).join(' - ')}
        </Typography>
      </Stack>
    </Paper>
  );
}

function TotalRow({ label, value, strong }: { label: string; value: string; strong?: boolean }) {
  return (
    <Stack direction="row" justifyContent="space-between" spacing={2}>
      <Typography color={strong ? 'text.primary' : 'text.secondary'} fontWeight={strong ? 800 : 500}>
        {label}
      </Typography>
      <Typography fontWeight={strong ? 800 : 700}>{value}</Typography>
    </Stack>
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
