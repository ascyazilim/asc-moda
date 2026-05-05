import { Stack, Typography } from '@mui/material';

import { formatCurrency } from '../../utils/formatters';

type PriceDisplayProps = {
  price: number;
  maxPrice?: number;
  compareAtPrice?: number;
  size?: 'sm' | 'md' | 'lg';
};

export function PriceDisplay({ price, maxPrice, compareAtPrice, size = 'md' }: PriceDisplayProps) {
  const priceVariant = size === 'lg' ? 'h4' : size === 'sm' ? 'body1' : 'h6';
  const hasRange = maxPrice != null && maxPrice > price;

  return (
    <Stack direction="row" spacing={1} alignItems="baseline">
      <Typography variant={priceVariant} fontWeight={700}>
        {hasRange ? `${formatCurrency(price)} - ${formatCurrency(maxPrice)}` : formatCurrency(price)}
      </Typography>
      {compareAtPrice ? (
        <Typography
          variant="body2"
          color="text.secondary"
          sx={{ textDecoration: 'line-through' }}
        >
          {formatCurrency(compareAtPrice)}
        </Typography>
      ) : null}
    </Stack>
  );
}
