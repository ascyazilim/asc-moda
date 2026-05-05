import AddIcon from '@mui/icons-material/Add';
import RemoveIcon from '@mui/icons-material/Remove';
import { IconButton, Stack, Typography } from '@mui/material';

import { clampQuantity } from '../../utils/formatters';

type QuantitySelectorProps = {
  value: number;
  onChange: (value: number) => void;
  min?: number;
  max?: number;
};

export function QuantitySelector({
  value,
  onChange,
  min = 1,
  max = 10,
}: QuantitySelectorProps) {
  const update = (nextValue: number) => {
    onChange(clampQuantity(nextValue, min, max));
  };

  return (
    <Stack
      direction="row"
      alignItems="center"
      sx={{
        width: 132,
        height: 44,
        border: 1,
        borderColor: 'divider',
        borderRadius: 999,
        bgcolor: 'background.paper',
      }}
    >
      <IconButton
        aria-label="Adedi azalt"
        size="small"
        onClick={() => update(value - 1)}
        disabled={value <= min}
        sx={{ mx: 0.5 }}
      >
        <RemoveIcon fontSize="small" />
      </IconButton>
      <Typography
        fontWeight={700}
        textAlign="center"
        sx={{
          flex: 1,
          minWidth: 24,
        }}
      >
        {value}
      </Typography>
      <IconButton
        aria-label="Adedi artır"
        size="small"
        onClick={() => update(value + 1)}
        disabled={value >= max}
        sx={{ mx: 0.5 }}
      >
        <AddIcon fontSize="small" />
      </IconButton>
    </Stack>
  );
}

