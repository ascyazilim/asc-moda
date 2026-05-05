import FilterListIcon from '@mui/icons-material/FilterList';
import {
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
import { useEffect, useMemo, useState } from 'react';
import { useSearchParams } from 'react-router-dom';

import { EmptyState } from '../../../components/common/EmptyState';
import { PageContainer } from '../../../components/common/PageContainer';
import { PageHero } from '../../../components/common/PageHero';
import { ProductCard } from '../../../components/ui/ProductCard';
import { useProducts } from '../../../hooks/useStorefrontQueries';
import { Product, ProductCategory, ProductFilters, ProductSort } from '../../../types/product';
import { storefrontCategories } from '../mock/storefrontData';
import { FilterPanel } from './components/FilterPanel';

const pageSize = 8;
const emptyProducts: Product[] = [];

const defaultFilters: ProductFilters = {
  category: 'all',
  priceRange: [0, 4000],
  sort: 'featured',
  colors: [],
  sizes: [],
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
  const [page, setPage] = useState(1);
  const [filters, setFilters] = useState<ProductFilters>(() => ({
    ...defaultFilters,
    category: getCategoryParam(searchParams.get('category')),
    sort: getSortParam(searchParams.get('sort')),
  }));

  useEffect(() => {
    setFilters((current) => ({
      ...current,
      category: getCategoryParam(searchParams.get('category')),
      sort: getSortParam(searchParams.get('sort')),
    }));
    setPage(1);
  }, [searchParams]);

  const productsQuery = useProducts(filters);
  const products = productsQuery.data?.items ?? emptyProducts;
  const pageCount = Math.max(1, Math.ceil(products.length / pageSize));
  const paginatedProducts = useMemo(() => {
    const start = (page - 1) * pageSize;

    return products.slice(start, start + pageSize);
  }, [page, products]);

  const handleFiltersChange = (nextFilters: ProductFilters) => {
    setFilters(nextFilters);
    setPage(1);
  };

  const resetFilters = () => {
    setFilters(defaultFilters);
    setPage(1);
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
            <FilterPanel filters={filters} onChange={handleFiltersChange} onReset={resetFilters} />
          </Box>

          <Stack spacing={3}>
            <Stack
              direction={{ xs: 'column', sm: 'row' }}
              spacing={1.5}
              justifyContent="space-between"
              alignItems={{ xs: 'stretch', sm: 'center' }}
            >
              <Typography color="text.secondary">
                {products.length} ürün listeleniyor
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

            {productsQuery.isLoading ? (
              <Stack alignItems="center" sx={{ py: 8 }}>
                <CircularProgress color="secondary" />
              </Stack>
            ) : paginatedProducts.length ? (
              <>
                <Grid container spacing={{ xs: 2, md: 3 }}>
                  {paginatedProducts.map((product) => (
                    <Grid item xs={6} sm={4} lg={3} key={product.id}>
                      <ProductCard product={product} />
                    </Grid>
                  ))}
                </Grid>
                <Stack alignItems="center" sx={{ pt: 2 }}>
                  <Pagination
                    count={pageCount}
                    page={page}
                    onChange={(_, value) => setPage(value)}
                    color="primary"
                  />
                </Stack>
              </>
            ) : (
              <EmptyState
                title="Bu filtrelerde ürün bulunamadı"
                description="Kategori, renk veya fiyat aralığını sadeleştirerek tekrar deneyebilirsiniz."
                actionLabel="Filtreleri Temizle"
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

function getCategoryParam(value: string | null): ProductCategory | 'all' {
  if (storefrontCategories.some((category) => category.id === value)) {
    return value as ProductCategory;
  }

  return 'all';
}

function getSortParam(value: string | null): ProductSort {
  if (sortOptions.some((option) => option.value === value)) {
    return value as ProductSort;
  }

  return 'featured';
}
