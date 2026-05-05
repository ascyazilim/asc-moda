import DeleteOutlineIcon from '@mui/icons-material/DeleteOutline';
import {
  Box,
  Button,
  Divider,
  IconButton,
  Paper,
  Stack,
  Typography,
} from '@mui/material';
import { useMemo, useState } from 'react';
import { Link as RouterLink } from 'react-router-dom';

import { EmptyState } from '../../../components/common/EmptyState';
import { PageContainer } from '../../../components/common/PageContainer';
import { PageHero } from '../../../components/common/PageHero';
import { CartSummaryCard } from '../../../components/ui/CartSummaryCard';
import { PriceDisplay } from '../../../components/ui/PriceDisplay';
import { QuantitySelector } from '../../../components/ui/QuantitySelector';
import { CartItem } from '../../../types/cart';
import { formatCurrency } from '../../../utils/formatters';
import { mockCartItems } from '../mock/storefrontData';

export function CartPage() {
  const [items, setItems] = useState<CartItem[]>(mockCartItems);
  const totals = useMemo(() => {
    const subtotal = items.reduce(
      (sum, item) => sum + item.product.price * item.quantity,
      0,
    );
    const discount = subtotal > 2500 ? 150 : 0;
    const shipping = subtotal > 1500 || subtotal === 0 ? 0 : 89;

    return {
      subtotal,
      discount,
      shipping,
      total: Math.max(0, subtotal - discount + shipping),
    };
  }, [items]);

  const updateQuantity = (itemId: string, quantity: number) => {
    setItems((current) =>
      current.map((item) => (item.id === itemId ? { ...item, quantity } : item)),
    );
  };

  const removeItem = (itemId: string) => {
    setItems((current) => current.filter((item) => item.id !== itemId));
  };

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
        {items.length ? (
          <Box
            sx={{
              display: 'grid',
              gridTemplateColumns: { xs: '1fr', lg: 'minmax(0, 1fr) 380px' },
              gap: { xs: 3, lg: 4 },
              alignItems: 'start',
            }}
          >
            <Stack spacing={2}>
              {items.map((item) => (
                <CartItemRow
                  key={item.id}
                  item={item}
                  onQuantityChange={(quantity) => updateQuantity(item.id, quantity)}
                  onRemove={() => removeItem(item.id)}
                />
              ))}
            </Stack>
            <CartSummaryCard totals={totals} />
          </Box>
        ) : (
          <EmptyState
            title="Sepetiniz boş"
            description="Asc Moda koleksiyonundan seçili parçaları sepete ekleyerek devam edebilirsiniz."
            actionLabel="Alışverişe Başla"
            actionHref="/products"
          />
        )}
      </PageContainer>
    </>
  );
}

type CartItemRowProps = {
  item: CartItem;
  onQuantityChange: (quantity: number) => void;
  onRemove: () => void;
};

function CartItemRow({ item, onQuantityChange, onRemove }: CartItemRowProps) {
  const lineTotal = item.product.price * item.quantity;

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
        <Box
          component={RouterLink}
          to={`/products/${item.product.slug}`}
          sx={{ alignSelf: 'stretch' }}
        >
          <Box
            component="img"
            src={item.product.images[0]}
            alt={item.product.name}
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
                to={`/products/${item.product.slug}`}
                variant="h5"
                sx={{ display: 'block' }}
              >
                {item.product.name}
              </Typography>
              <Typography variant="body2" color="text.secondary">
                {item.color} / {item.size}
              </Typography>
            </Box>
            <IconButton aria-label="Ürünü sepetten çıkar" onClick={onRemove}>
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
              max={item.product.stock}
            />
            <Stack spacing={0.25}>
              <PriceDisplay price={item.product.price} size="sm" />
              <Typography variant="body2" color="text.secondary">
                Satır toplamı: {formatCurrency(lineTotal)}
              </Typography>
            </Stack>
          </Stack>
        </Stack>

        <Stack spacing={1.25} alignItems="flex-end" sx={{ display: { xs: 'none', md: 'flex' } }}>
          <Typography variant="body2" color="text.secondary">
            Satır toplamı
          </Typography>
          <Typography variant="h5">{formatCurrency(lineTotal)}</Typography>
          <Button size="small" color="inherit" onClick={onRemove}>
            Sil
          </Button>
        </Stack>
      </Box>
    </Paper>
  );
}

