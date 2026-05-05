import {
  Alert,
  Box,
  Button,
  CircularProgress,
  FormControl,
  Grid,
  InputLabel,
  MenuItem,
  Pagination,
  Select,
  Stack,
  TextField,
  Typography,
} from '@mui/material';
import { FormEvent, useEffect, useMemo, useState } from 'react';
import { useSearchParams } from 'react-router-dom';

import { EmptyState } from '../../../components/common/EmptyState';
import { PageContainer } from '../../../components/common/PageContainer';
import { PageHero } from '../../../components/common/PageHero';
import { ProductCard } from '../../../components/ui/ProductCard';
import { SearchInput } from '../../../components/ui/SearchInput';
import { useCategories, useSearchResults } from '../../../hooks/useStorefrontQueries';
import { ProductFilters, ProductSort } from '../../../types/product';
import { storefrontCategories } from '../mock/storefrontData';

const pageSize = 12;

const sortOptions: Array<{ value: ProductSort; label: string }> = [
  { value: 'featured', label: 'Öne çıkanlar' },
  { value: 'newest', label: 'En yeniler' },
  { value: 'priceAsc', label: 'Fiyat artan' },
  { value: 'priceDesc', label: 'Fiyat azalan' },
];

export function SearchPage() {
  const [searchParams, setSearchParams] = useSearchParams();
  const urlQuery = searchParams.get('q') ?? '';
  const [query, setQuery] = useState(urlQuery);
  const [minPrice, setMinPrice] = useState(searchParams.get('minPrice') ?? '');
  const [maxPrice, setMaxPrice] = useState(searchParams.get('maxPrice') ?? '');
  const categoriesQuery = useCategories();
  const categories = categoriesQuery.data?.length ? categoriesQuery.data : storefrontCategories;

  useEffect(() => {
    setQuery(urlQuery);
    setMinPrice(searchParams.get('minPrice') ?? '');
    setMaxPrice(searchParams.get('maxPrice') ?? '');
  }, [searchParams, urlQuery]);

  const filters = useMemo<ProductFilters>(
    () => ({
      categorySlug: searchParams.get('categorySlug') ?? 'all',
      priceRange: getPriceRange(searchParams.get('minPrice'), searchParams.get('maxPrice')),
      sort: getSortParam(searchParams.get('sort')),
      page: getPageParam(searchParams.get('page')),
      size: pageSize,
    }),
    [searchParams],
  );

  const resultsQuery = useSearchResults(urlQuery, filters);
  const results = resultsQuery.data?.items ?? [];
  const total = resultsQuery.data?.total ?? 0;
  const pageCount = Math.max(1, resultsQuery.data?.totalPages ?? 1);

  const submitSearch = (event?: FormEvent<HTMLFormElement>) => {
    event?.preventDefault();
    updateParams({
      q: query.trim(),
      page: '1',
    });
  };

  const applyPriceFilter = () => {
    updateParams({
      minPrice,
      maxPrice,
      page: '1',
    });
  };

  const updateParams = (patch: Record<string, string>) => {
    const next = new URLSearchParams(searchParams);

    Object.entries(patch).forEach(([key, value]) => {
      if (value.trim()) {
        next.set(key, value.trim());
      } else {
        next.delete(key);
      }
    });

    setSearchParams(next);
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

          <Box
            sx={{
              display: 'grid',
              gridTemplateColumns: {
                xs: '1fr',
                sm: '1fr 1fr',
                lg: '1.2fr 0.8fr 0.8fr 0.9fr auto',
              },
              gap: 1.5,
              alignItems: 'start',
            }}
          >
            <FormControl size="small">
              <InputLabel id="search-category-label">Kategori</InputLabel>
              <Select
                labelId="search-category-label"
                label="Kategori"
                value={filters.categorySlug ?? 'all'}
                onChange={(event) =>
                  updateParams({
                    categorySlug: event.target.value === 'all' ? '' : event.target.value,
                    page: '1',
                  })
                }
              >
                <MenuItem value="all">Tüm Kategoriler</MenuItem>
                {categories.map((category) => (
                  <MenuItem key={category.slug} value={category.slug}>
                    {category.title}
                  </MenuItem>
                ))}
              </Select>
            </FormControl>
            <TextField
              size="small"
              label="Min. fiyat"
              value={minPrice}
              onChange={(event) => setMinPrice(event.target.value)}
              inputProps={{ inputMode: 'numeric' }}
            />
            <TextField
              size="small"
              label="Maks. fiyat"
              value={maxPrice}
              onChange={(event) => setMaxPrice(event.target.value)}
              inputProps={{ inputMode: 'numeric' }}
            />
            <FormControl size="small">
              <InputLabel id="search-sort-label">Sıralama</InputLabel>
              <Select
                labelId="search-sort-label"
                label="Sıralama"
                value={filters.sort ?? 'featured'}
                onChange={(event) =>
                  updateParams({
                    sort: event.target.value,
                    page: '1',
                  })
                }
              >
                {sortOptions.map((option) => (
                  <MenuItem key={option.value} value={option.value}>
                    {option.label}
                  </MenuItem>
                ))}
              </Select>
            </FormControl>
            <Button variant="outlined" onClick={applyPriceFilter}>
              Uygula
            </Button>
          </Box>

          <Typography color="text.secondary">
            {urlQuery ? `"${urlQuery}" için ${total} sonuç` : `${total} ürün`}
          </Typography>

          {resultsQuery.isError ? (
            <Alert severity="error">
              Arama sonuçları yüklenemedi. Lütfen backend servisinin çalıştığından emin olun.
            </Alert>
          ) : null}

          {resultsQuery.isLoading ? (
            <Stack alignItems="center" sx={{ py: 8 }}>
              <CircularProgress color="secondary" />
            </Stack>
          ) : results.length ? (
            <>
              <Grid container spacing={{ xs: 2, md: 3 }}>
                {results.map((product) => (
                  <Grid item xs={6} sm={4} md={3} key={product.id}>
                    <ProductCard product={product} />
                  </Grid>
                ))}
              </Grid>
              <Stack alignItems="center" sx={{ pt: 2 }}>
                <Pagination
                  count={pageCount}
                  page={filters.page ?? 1}
                  onChange={(_, value) => updateParams({ page: String(value) })}
                  color="primary"
                />
              </Stack>
            </>
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

function getSortParam(value: string | null): ProductSort {
  if (sortOptions.some((option) => option.value === value)) {
    return value as ProductSort;
  }

  return 'featured';
}

function getPageParam(value: string | null) {
  const parsed = Number(value);

  return Number.isFinite(parsed) && parsed > 0 ? parsed : 1;
}

function getPriceRange(minPrice: string | null, maxPrice: string | null): [number, number] | undefined {
  const min = Number(minPrice);
  const max = Number(maxPrice);

  if (Number.isFinite(min) && Number.isFinite(max) && minPrice && maxPrice) {
    return [min, max];
  }

  if (Number.isFinite(min) && minPrice) {
    return [min, 999999];
  }

  if (Number.isFinite(max) && maxPrice) {
    return [0, max];
  }

  return undefined;
}

