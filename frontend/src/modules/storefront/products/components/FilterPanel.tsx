import RestartAltIcon from '@mui/icons-material/RestartAlt';
import {
  Box,
  Button,
  Checkbox,
  Divider,
  FormControl,
  FormControlLabel,
  FormGroup,
  FormLabel,
  Radio,
  RadioGroup,
  Slider,
  Stack,
  Typography,
} from '@mui/material';

import { Category, ProductFilters } from '../../../../types/product';
import { formatCurrency } from '../../../../utils/formatters';

const colorOptions = ['Vizon', 'Ivory', 'Moka', 'Krem', 'Bej', 'Sage', 'Nude'];
const sizeOptions = ['Standart', '36', '38', '40', '42', '44', 'S', 'M', 'L', 'XL'];

type FilterPanelProps = {
  filters: ProductFilters;
  categories: Category[];
  onChange: (filters: ProductFilters) => void;
  onReset: () => void;
};

export function FilterPanel({ filters, categories, onChange, onReset }: FilterPanelProps) {
  const priceRange = filters.priceRange ?? [0, 4000];

  const updateFilter = (patch: ProductFilters) => {
    onChange({
      ...filters,
      ...patch,
    });
  };

  const toggleArrayValue = (key: 'colors' | 'sizes', value: string) => {
    const current = filters[key] ?? [];
    const next = current.includes(value)
      ? current.filter((item) => item !== value)
      : [...current, value];

    updateFilter({
      [key]: next,
    });
  };

  return (
    <Stack spacing={3}>
      <Stack direction="row" justifyContent="space-between" alignItems="center" spacing={2}>
        <Typography variant="h5">Filtreler</Typography>
        <Button size="small" startIcon={<RestartAltIcon />} onClick={onReset}>
          Temizle
        </Button>
      </Stack>

      <Divider />

      <FormControl>
        <FormLabel>Kategori</FormLabel>
        <RadioGroup
          value={filters.categorySlug ?? 'all'}
          onChange={(event) =>
            updateFilter({
              categorySlug: event.target.value,
            })
          }
        >
          <FormControlLabel value="all" control={<Radio />} label="Tüm Ürünler" />
          {categories.map((category) => (
            <FormControlLabel
              key={category.slug}
              value={category.slug}
              control={<Radio />}
              label={category.title}
            />
          ))}
        </RadioGroup>
      </FormControl>

      <Divider />

      <Box>
        <Stack direction="row" justifyContent="space-between" spacing={2} sx={{ mb: 1 }}>
          <Typography fontWeight={700}>Fiyat Aralığı</Typography>
          <Typography variant="body2" color="text.secondary">
            {formatCurrency(priceRange[0])} - {formatCurrency(priceRange[1])}
          </Typography>
        </Stack>
        <Slider
          value={priceRange}
          min={0}
          max={4000}
          step={100}
          onChange={(_, value) => updateFilter({ priceRange: value as [number, number] })}
          valueLabelDisplay="auto"
        />
      </Box>

      <Divider />

      <FormControl>
        <FormLabel>Renk</FormLabel>
        <FormGroup>
          {colorOptions.map((color) => (
            <FormControlLabel
              key={color}
              control={
                <Checkbox
                  checked={filters.colors?.includes(color) ?? false}
                  onChange={() => toggleArrayValue('colors', color)}
                />
              }
              label={color}
            />
          ))}
        </FormGroup>
      </FormControl>

      <Divider />

      <FormControl>
        <FormLabel>Beden</FormLabel>
        <Box
          sx={{
            display: 'grid',
            gridTemplateColumns: 'repeat(2, minmax(0, 1fr))',
            gap: 0.5,
          }}
        >
          {sizeOptions.map((size) => (
            <FormControlLabel
              key={size}
              control={
                <Checkbox
                  checked={filters.sizes?.includes(size) ?? false}
                  onChange={() => toggleArrayValue('sizes', size)}
                />
              }
              label={size}
            />
          ))}
        </Box>
      </FormControl>
    </Stack>
  );
}
