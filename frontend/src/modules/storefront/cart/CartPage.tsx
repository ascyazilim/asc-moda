import DeleteOutlineIcon from '@mui/icons-material/DeleteOutline';
import {
  Alert,
  Box,
  Button,
  CircularProgress,
  Divider,
  IconButton,
  Paper,
  Stack,
  Typography,
} from '@mui/material';
import { useMemo } from 'react';
import { Link as RouterLink } from 'react-router-dom';

import { EmptyState } from '../../../components/common/EmptyState';
import { PageContainer } from '../../../components/common/PageContainer';
import { PageHero } from '../../../components/common/PageHero';
import { CartSummaryCard } from '../../../components/ui/CartSummaryCard';
import { PriceDisplay } from '../../../components/ui/PriceDisplay';
import { QuantitySelector } from '../../../components/ui/QuantitySelector';
import {
  useCart,
  useCartSummary,
  useClearCartMutation,
  useRemoveCartItemMutation,
  useUpdateCartItemQuantityMutation,
} from '../../../hooks/useStorefrontQueries';
import { CartItem, CartTotals } from '../../../types/cart';
import { formatCurrency } from '../../../utils/formatters';

export function CartPage() {
  const cartQuery = useCart();
  const summaryQuery = useCartSummary();
  const updateQuantityMutation = useUpdateCartItemQuantityMutation();
  const removeItemMutation = useRemoveCartItemMutation();
  const clearCartMutation = useClearCartMutation();
  const items = cartQuery.data?.items ?? [];
  const totals = useMemo<CartTotals>(() => {
    const selectedTotal = summaryQuery.data?.selectedTotal ?? cartQuery.data?.selectedTotal ?? 0;

    return {
      subtotal: selectedTotal,
      discount: 0,
      shipping: selectedTotal > 0 ? 0 : 0,
      total: selectedTotal,
    };
  }, [cartQuery.data?.selectedTotal, summaryQuery.data?.selectedTotal]);

  const isMutating =
    updateQuantityMutation.isPending ||
    removeItemMutation.isPending ||
    clearCartMutation.isPending;

  return (
    <>
      <PageContainer roomy={false}>
        <PageHero
          eyebrow="Sepet"
          title="Alışveriş sepeti"
          description="Seçtiğiniz parçaları gözden geçirin; checkout entegrasyonu sonraki aşama için hazır bırakıldı."
        />
      </PageContainer>

      <PageContainer>
        {cartQuery.isLoading ? (
          <Stack alignItems="center" sx={{ py: 8 }}>
            <CircularProgress color="secondary" />
          </Stack>
        ) : null}

        {cartQuery.isError ? (
          <Alert severity="error" sx={{ mb: 3 }}>
            Sepet bilgisi yüklenemedi. Demo müşteri kimliği ve cart servisinin çalıştığından emin olun.
          </Alert>
        ) : null}

        {!cartQuery.isLoading && items.length ? (
          <Box
            sx={{
              display: 'grid',
              gridTemplateColumns: { xs: '1fr', lg: 'minmax(0, 1fr) 380px' },
              gap: { xs: 3, lg: 4 },
              alignItems: 'start',
            }}
          >
            <Stack spacing={2}>
              <Stack direction="row" justifyContent="space-between" alignItems="center" spacing={2}>
                <Typography color="text.secondary">
                  {cartQuery.data?.totalQuantity ?? 0} ürün sepetinizde
                </Typography>
                <Button
                  color="inherit"
                  onClick={() => clearCartMutation.mutate()}
                  disabled={isMutating}
                >
                  Sepeti Temizle
                </Button>
              </Stack>
              {items.map((item) => (
                <CartItemRow
                  key={item.id}
                  item={item}
                  disabled={isMutating}
                  onQuantityChange={(quantity) =>
                    updateQuantityMutation.mutate({
                      itemId: item.id,
                      quantity,
                    })
                  }
                  onRemove={() => removeItemMutation.mutate(item.id)}
                />
              ))}
            </Stack>
            <CartSummaryCard totals={totals} />
          </Box>
        ) : null}

        {!cartQuery.isLoading && !items.length && !cartQuery.isError ? (
          <EmptyState
            title="Sepetiniz boş"
            description="Asc Moda koleksiyonundan seçili parçaları sepete ekleyerek devam edebilirsiniz."
            actionLabel="Alışverişe Başla"
            actionHref="/products"
          />
        ) : null}
      </PageContainer>
    </>
  );
}

type CartItemRowProps = {
  item: CartItem;
  disabled?: boolean;
  onQuantityChange: (quantity: number) => void;
  onRemove: () => void;
};

function CartItemRow({ item, disabled, onQuantityChange, onRemove }: CartItemRowProps) {
  return (
    <Paper
      elevation={0}
      sx={{
        border: 1,
        borderColor: 'divider',
        bgcolor: 'background.paper',
        p: { xs: 1.5, sm: 2 },
      }}
    >
      <Box
        sx={{
          display: 'grid',
          gridTemplateColumns: {
            xs: '96px 1fr',
            md: '128px minmax(0, 1fr) auto',
          },
          gap: { xs: 1.5, md: 2.5 },
          alignItems: 'center',
        }}
      >
        <Box component={RouterLink} to={`/products/${item.productSlug}`} sx={{ alignSelf: 'stretch' }}>
          <Box
            component="img"
            src={item.imageUrl}
            alt={item.productName}
            sx={{
              width: '100%',
              height: { xs: 124, md: 154 },
              objectFit: 'cover',
              borderRadius: 1.5,
              bgcolor: 'secondary.light',
            }}
          />
        </Box>

        <Stack spacing={1.25} sx={{ minWidth: 0 }}>
          <Stack direction="row" justifyContent="space-between" alignItems="flex-start" spacing={2}>
            <Box>
              <Typography
                component={RouterLink}
                to={`/products/${item.productSlug}`}
                variant="h5"
                sx={{ display: 'block' }}
              >
                {item.productName}
              </Typography>
              <Typography variant="body2" color="text.secondary">
                {[item.color, item.size].filter(Boolean).join(' / ') || item.variantName || item.sku}
              </Typography>
            </Box>
            <IconButton aria-label="Ürünü sepetten çıkar" onClick={onRemove} disabled={disabled}>
              <DeleteOutlineIcon />
            </IconButton>
          </Stack>

          <Divider sx={{ display: { md: 'none' } }} />

          <Stack
            direction={{ xs: 'column', sm: 'row' }}
            spacing={1.5}
            alignItems={{ xs: 'flex-start', sm: 'center' }}
            justifyContent="space-between"
          >
            <QuantitySelector
              value={item.quantity}
              onChange={onQuantityChange}
              max={10}
              disabled={disabled}
            />
            <Stack spacing={0.25}>
              <PriceDisplay price={item.unitPrice} size="sm" />
              <Typography variant="body2" color="text.secondary">
                Satır toplamı: {formatCurrency(item.lineTotal)}
              </Typography>
            </Stack>
          </Stack>
        </Stack>

        <Stack spacing={1.25} alignItems="flex-end" sx={{ display: { xs: 'none', md: 'flex' } }}>
          <Typography variant="body2" color="text.secondary">
            Satır toplamı
          </Typography>
          <Typography variant="h5">{formatCurrency(item.lineTotal)}</Typography>
          <Button size="small" color="inherit" onClick={onRemove} disabled={disabled}>
            Sil
          </Button>
        </Stack>
      </Box>
    </Paper>
  );
}
