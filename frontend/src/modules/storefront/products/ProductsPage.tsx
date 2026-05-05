import FilterListIcon from '@mui/icons-material/FilterList';
import {
  Alert,
  Box,
  Button,
  CircularProgress,
  Drawer,
  FormControl,
  Grid,
  InputLabel,
  MenuItem,
  Pagination,
  Select,
  Stack,
  Typography,
} from '@mui/material';
import { useEffect, useState } from 'react';
import { useSearchParams } from 'react-router-dom';

import { EmptyState } from '../../../components/common/EmptyState';
import { PageContainer } from '../../../components/common/PageContainer';
import { PageHero } from '../../../components/common/PageHero';
import { ProductCard } from '../../../components/ui/ProductCard';
import { useCategories, useProducts } from '../../../hooks/useStorefrontQueries';
import { ProductFilters, ProductSort } from '../../../types/product';
import { storefrontCategories } from '../mock/storefrontData';
import { FilterPanel } from './components/FilterPanel';

const pageSize = 8;

const defaultFilters: ProductFilters = {
  categorySlug: 'all',
  priceRange: [0, 4000],
  sort: 'featured',
  colors: [],
  sizes: [],
  page: 1,
  size: pageSize,
};

const sortOptions: Array<{ value: ProductSort; label: string }> = [
  { value: 'featured', label: 'Öne çıkanlar' },
  { value: 'newest', label: 'En yeniler' },
  { value: 'priceAsc', label: 'Fiyat artan' },
  { value: 'priceDesc', label: 'Fiyat azalan' },
];

export function ProductsPage() {
  const [searchParams] = useSearchParams();
  const [mobileFiltersOpen, setMobileFiltersOpen] = useState(false);
  const [filters, setFilters] = useState<ProductFilters>(() => ({
    ...defaultFilters,
    categorySlug: getCategoryParam(searchParams.get('categorySlug') ?? searchParams.get('category')),
    sort: getSortParam(searchParams.get('sort')),
    page: getPageParam(searchParams.get('page')),
  }));

  useEffect(() => {
    setFilters((current) => ({
      ...current,
      categorySlug: getCategoryParam(searchParams.get('categorySlug') ?? searchParams.get('category')),
      sort: getSortParam(searchParams.get('sort')),
      page: getPageParam(searchParams.get('page')),
    }));
  }, [searchParams]);

  const categoriesQuery = useCategories();
  const categories = categoriesQuery.data?.length ? categoriesQuery.data : storefrontCategories;
  const productsQuery = useProducts(filters);
  const products = productsQuery.data?.items ?? [];
  const total = productsQuery.data?.total ?? 0;
  const page = filters.page ?? 1;
  const pageCount = Math.max(1, productsQuery.data?.totalPages ?? 1);

  const handleFiltersChange = (nextFilters: ProductFilters) => {
    setFilters({
      ...nextFilters,
      page: 1,
      size: pageSize,
    });
  };

  const resetFilters = () => {
    setFilters(defaultFilters);
  };

  return (
    <>
      <PageContainer roomy={false}>
        <PageHero
          eyebrow="Koleksiyon"
          title="Tüm ürünler"
          description="Başörtüden dış giyime, Asc Moda'nın sade ve premium ürün seçkisini filtreleyin."
        />
      </PageContainer>

      <PageContainer>
        <Box
          sx={{
            display: 'grid',
            gridTemplateColumns: { xs: '1fr', md: '280px 1fr', xl: '320px 1fr' },
            gap: { xs: 3, md: 4 },
            alignItems: 'start',
          }}
        >
          <Box
            sx={{
              display: { xs: 'none', md: 'block' },
              border: 1,
              borderColor: 'divider',
              bgcolor: 'background.paper',
              p: 3,
              position: 'sticky',
              top: 104,
            }}
          >
            <FilterPanel
              filters={filters}
              categories={categories}
              onChange={handleFiltersChange}
              onReset={resetFilters}
            />
          </Box>

          <Stack spacing={3}>
            <Stack
              direction={{ xs: 'column', sm: 'row' }}
              spacing={1.5}
              justifyContent="space-between"
              alignItems={{ xs: 'stretch', sm: 'center' }}
            >
              <Typography color="text.secondary">
                {productsQuery.isLoading ? 'Ürünler yükleniyor' : `${total} ürün listeleniyor`}
              </Typography>
              <Stack direction="row" spacing={1.5}>
                <Button
                  variant="outlined"
                  startIcon={<FilterListIcon />}
                  onClick={() => setMobileFiltersOpen(true)}
                  sx={{ display: { md: 'none' } }}
                >
                  Filtrele
                </Button>
                <FormControl size="small" sx={{ minWidth: 190 }}>
                  <InputLabel id="sort-label">Sıralama</InputLabel>
                  <Select
                    labelId="sort-label"
                    label="Sıralama"
                    value={filters.sort ?? 'featured'}
                    onChange={(event) =>
                      handleFiltersChange({
                        ...filters,
                        sort: event.target.value as ProductSort,
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
              </Stack>
            </Stack>

            {productsQuery.isError ? (
              <Alert severity="error">
                Ürünler yüklenemedi. Lütfen backend servisinin çalıştığından emin olun.
              </Alert>
            ) : null}

            {productsQuery.isLoading ? (
              <Stack alignItems="center" sx={{ py: 8 }}>
                <CircularProgress color="secondary" />
              </Stack>
            ) : products.length ? (
              <>
                <Grid container spacing={{ xs: 2, md: 3 }}>
                  {products.map((product) => (
                    <Grid item xs={6} sm={4} lg={3} key={product.id}>
                      <ProductCard product={product} />
                    </Grid>
                  ))}
                </Grid>
                <Stack alignItems="center" sx={{ pt: 2 }}>
                  <Pagination
                    count={pageCount}
                    page={page}
                    onChange={(_, value) =>
                      setFilters((current) => ({
                        ...current,
                        page: value,
                      }))
                    }
                    color="primary"
                  />
                </Stack>
              </>
            ) : (
              <EmptyState
                title="Bu filtrelerde ürün bulunamadı"
                description="Kategori, renk veya fiyat aralığını sadeleştirerek tekrar deneyebilirsiniz."
                actionLabel="Tüm Ürünler"
                actionHref="/products"
              />
            )}
          </Stack>
        </Box>
      </PageContainer>

      <Drawer
        anchor="bottom"
        open={mobileFiltersOpen}
        onClose={() => setMobileFiltersOpen(false)}
        PaperProps={{
          sx: {
            maxHeight: '86vh',
            borderTopLeftRadius: 16,
            borderTopRightRadius: 16,
            p: 2.5,
          },
        }}
      >
        <FilterPanel
          filters={filters}
          categories={categories}
          onChange={handleFiltersChange}
          onReset={resetFilters}
        />
        <Button
          variant="contained"
          fullWidth
          sx={{ mt: 3 }}
          onClick={() => setMobileFiltersOpen(false)}
        >
          Ürünleri Göster
        </Button>
      </Drawer>
    </>
  );
}

function getCategoryParam(value: string | null): string | 'all' {
  return value && value.trim() ? value : 'all';
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

