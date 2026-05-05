import TuneIcon from '@mui/icons-material/Tune';
import {
  Box,
  Button,
  CircularProgress,
  FormControl,
  Grid,
  InputLabel,
  MenuItem,
  Select,
  Stack,
  Typography,
} from '@mui/material';
import { FormEvent, useEffect, useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';

import { EmptyState } from '../../../components/common/EmptyState';
import { PageContainer } from '../../../components/common/PageContainer';
import { PageHero } from '../../../components/common/PageHero';
import { ProductCard } from '../../../components/ui/ProductCard';
import { SearchInput } from '../../../components/ui/SearchInput';
import { useSearchResults } from '../../../hooks/useStorefrontQueries';
import { ProductFilters, ProductSort } from '../../../types/product';

const sortOptions: Array<{ value: ProductSort; label: string }> = [
  { value: 'featured', label: 'Öne çıkanlar' },
  { value: 'newest', label: 'En yeniler' },
  { value: 'priceAsc', label: 'Fiyat artan' },
  { value: 'priceDesc', label: 'Fiyat azalan' },
];

export function SearchPage() {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const urlQuery = searchParams.get('q') ?? '';
  const [query, setQuery] = useState(urlQuery);
  const [filters, setFilters] = useState<ProductFilters>({
    sort: 'featured',
  });
  const resultsQuery = useSearchResults(urlQuery, filters);
  const results = resultsQuery.data?.items ?? [];

  useEffect(() => {
    setQuery(urlQuery);
  }, [urlQuery]);

  const submitSearch = (event?: FormEvent<HTMLFormElement>) => {
    event?.preventDefault();
    const trimmed = query.trim();
    navigate(trimmed ? `/search?q=${encodeURIComponent(trimmed)}` : '/search');
  };

  return (
    <>
      <PageContainer roomy={false}>
        <PageHero
          eyebrow="Arama"
          title="Koleksiyonda ara"
          description="Ürün adı, kategori, renk ya da stil hissiyle arama yapın."
        />
      </PageContainer>

      <PageContainer>
        <Stack spacing={3.5}>
          <Box
            component="form"
            onSubmit={submitSearch}
            sx={{
              display: 'grid',
              gridTemplateColumns: { xs: '1fr', md: '1fr auto' },
              gap: 1.5,
            }}
          >
            <SearchInput
              value={query}
              onChange={(event) => setQuery(event.target.value)}
              onSearch={() => submitSearch()}
              autoFocus
            />
            <Button type="submit" variant="contained" size="large">
              Ara
            </Button>
          </Box>

          <Stack
            direction={{ xs: 'column', sm: 'row' }}
            spacing={1.5}
            justifyContent="space-between"
            alignItems={{ xs: 'stretch', sm: 'center' }}
          >
            <Typography color="text.secondary">
              {urlQuery ? `"${urlQuery}" için ${results.length} sonuç` : `${results.length} ürün`}
            </Typography>
            <Stack direction="row" spacing={1.5}>
              <Button variant="outlined" startIcon={<TuneIcon />}>
                Filtre
              </Button>
              <FormControl size="small" sx={{ minWidth: 190 }}>
                <InputLabel id="search-sort-label">Sıralama</InputLabel>
                <Select
                  labelId="search-sort-label"
                  label="Sıralama"
                  value={filters.sort ?? 'featured'}
                  onChange={(event) =>
                    setFilters((current) => ({
                      ...current,
                      sort: event.target.value as ProductSort,
                    }))
                  }
                >
                  {sortOptions.map((option) => (
                    <MenuItem key={option.value} value={option.value}>
                      {option.label}
                    </MenuItem>
                  ))}
                </Select>
              </FormControl>
            </Stack>
          </Stack>

          {resultsQuery.isLoading ? (
            <Stack alignItems="center" sx={{ py: 8 }}>
              <CircularProgress color="secondary" />
            </Stack>
          ) : results.length ? (
            <Grid container spacing={{ xs: 2, md: 3 }}>
              {results.map((product) => (
                <Grid item xs={6} sm={4} md={3} key={product.id}>
                  <ProductCard product={product} />
                </Grid>
              ))}
            </Grid>
          ) : (
            <EmptyState
              title="Sonuç bulunamadı"
              description="Daha genel bir ürün adı, kategori veya renk arayarak tekrar deneyin."
              actionLabel="Tüm Ürünlere Git"
              actionHref="/products"
            />
          )}
        </Stack>
      </PageContainer>
    </>
  );
}

