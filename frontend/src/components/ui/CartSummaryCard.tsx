import { Button, Divider, Paper, Stack, Typography } from '@mui/material';
import { Link as RouterLink } from 'react-router-dom';

import { CartTotals } from '../../types/cart';
import { formatCurrency } from '../../utils/formatters';

type CartSummaryCardProps = {
  totals: CartTotals;
};

export function CartSummaryCard({ totals }: CartSummaryCardProps) {
  return (
    <Paper
      elevation={0}
      sx={{
        border: 1,
        borderColor: 'divider',
        p: { xs: 2.5, sm: 3 },
        position: { md: 'sticky' },
        top: { md: 104 },
      }}
    >
      <Stack spacing={2.2}>
        <Typography variant="h4">Sipariş Özeti</Typography>
        <Stack spacing={1.4}>
          <SummaryRow label="Ara toplam" value={formatCurrency(totals.subtotal)} />
          <SummaryRow label="İndirim" value={`-${formatCurrency(totals.discount)}`} />
          <SummaryRow
            label="Kargo"
            value={totals.shipping === 0 ? 'Ücretsiz' : formatCurrency(totals.shipping)}
          />
        </Stack>
        <Divider />
        <SummaryRow label="Toplam" value={formatCurrency(totals.total)} strong />
        <Button variant="contained" size="large" fullWidth>
          Siparişi Tamamla
        </Button>
        <Button component={RouterLink} to="/products" variant="outlined" fullWidth>
          Alışverişe Devam Et
        </Button>
      </Stack>
    </Paper>
  );
}

type SummaryRowProps = {
  label: string;
  value: string;
  strong?: boolean;
};

function SummaryRow({ label, value, strong }: SummaryRowProps) {
  return (
    <Stack direction="row" justifyContent="space-between" spacing={2}>
      <Typography color={strong ? 'text.primary' : 'text.secondary'} fontWeight={strong ? 700 : 500}>
        {label}
      </Typography>
      <Typography fontWeight={strong ? 800 : 600}>{value}</Typography>
    </Stack>
  );
}

