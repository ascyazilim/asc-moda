import AddShoppingCartIcon from '@mui/icons-material/AddShoppingCart';
import FavoriteBorderIcon from '@mui/icons-material/FavoriteBorder';
import {
  Alert,
  Box,
  Breadcrumbs,
  Button,
  Chip,
  CircularProgress,
  Divider,
  Grid,
  Link,
  Snackbar,
  Stack,
  Tab,
  Tabs,
  ToggleButton,
  ToggleButtonGroup,
  Typography,
} from '@mui/material';
import { useEffect, useMemo, useState } from 'react';
import { Link as RouterLink, useParams } from 'react-router-dom';

import { EmptyState } from '../../../components/common/EmptyState';
import { PageContainer } from '../../../components/common/PageContainer';
import { SectionHeader } from '../../../components/common/SectionHeader';
import { PriceDisplay } from '../../../components/ui/PriceDisplay';
import { ProductCard } from '../../../components/ui/ProductCard';
import { QuantitySelector } from '../../../components/ui/QuantitySelector';
import { useProductDetail } from '../../../hooks/useStorefrontQueries';
import { storefrontProducts } from '../mock/storefrontData';

export function ProductDetailPage() {
  const { slug } = useParams();
  const productQuery = useProductDetail(slug);
  const product = productQuery.data;
  const [activeImage, setActiveImage] = useState('');
  const [selectedColor, setSelectedColor] = useState('');
  const [selectedSize, setSelectedSize] = useState('');
  const [quantity, setQuantity] = useState(1);
  const [tab, setTab] = useState(0);
  const [snackbarOpen, setSnackbarOpen] = useState(false);

  useEffect(() => {
    if (product) {
      setActiveImage(product.images[0]);
      setSelectedColor(product.colors[0]?.name ?? '');
      setSelectedSize(product.sizes[0] ?? '');
      setQuantity(1);
    }
  }, [product]);

  const relatedProducts = useMemo(() => {
    if (!product) {
      return [];
    }

    return storefrontProducts
      .filter((item) => item.category === product.category && item.id !== product.id)
      .slice(0, 4);
  }, [product]);

  if (productQuery.isLoading) {
    return (
      <PageContainer>
        <Stack alignItems="center" sx={{ py: 10 }}>
          <CircularProgress color="secondary" />
        </Stack>
      </PageContainer>
    );
  }

  if (!product) {
    return (
      <PageContainer>
        <EmptyState
          title="Ürün bulunamadı"
          description="Bu ürün yayından kaldırılmış ya da adresi değişmiş olabilir."
          actionLabel="Ürünlere Dön"
          actionHref="/products"
        />
      </PageContainer>
    );
  }

  return (
    <PageContainer>
      <Stack spacing={{ xs: 3.5, md: 6 }}>
        <Breadcrumbs sx={{ color: 'text.secondary' }}>
          <Link component={RouterLink} to="/">
            Ana Sayfa
          </Link>
          <Link component={RouterLink} to="/products">
            Ürünler
          </Link>
          <Typography color="text.primary">{product.name}</Typography>
        </Breadcrumbs>

        <Grid container spacing={{ xs: 3, md: 5, lg: 7 }}>
          <Grid item xs={12} md={6.5}>
            <Stack spacing={2}>
              <Box
                component="img"
                src={activeImage}
                alt={product.name}
                sx={{
                  width: '100%',
                  aspectRatio: { xs: '4 / 5', md: '5 / 6' },
                  objectFit: 'cover',
                  borderRadius: 2,
                  bgcolor: 'secondary.light',
                }}
              />
              <Stack direction="row" spacing={1.25} sx={{ overflowX: 'auto', pb: 0.5 }}>
                {product.images.map((image) => (
                  <Box
                    key={image}
                    component="button"
                    type="button"
                    onClick={() => setActiveImage(image)}
                    sx={{
                      p: 0,
                      border: 2,
                      borderColor: activeImage === image ? 'primary.main' : 'transparent',
                      borderRadius: 1.5,
                      bgcolor: 'transparent',
                      cursor: 'pointer',
                      flex: '0 0 88px',
                    }}
                  >
                    <Box
                      component="img"
                      src={image}
                      alt={`${product.name} görseli`}
                      sx={{
                        width: 84,
                        height: 104,
                        objectFit: 'cover',
                        borderRadius: 1,
                      }}
                    />
                  </Box>
                ))}
              </Stack>
            </Stack>
          </Grid>

          <Grid item xs={12} md={5.5}>
            <Stack spacing={3} sx={{ position: { md: 'sticky' }, top: { md: 104 } }}>
              <Stack spacing={1.5}>
                <Stack direction="row" spacing={1} alignItems="center">
                  <Chip label={product.categoryLabel} color="secondary" />
                  {product.isNew ? <Chip label="Yeni" variant="outlined" /> : null}
                </Stack>
                <Typography variant="h1">{product.name}</Typography>
                <Typography color="text.secondary">{product.description}</Typography>
                <PriceDisplay
                  price={product.price}
                  compareAtPrice={product.compareAtPrice}
                  size="lg"
                />
              </Stack>

              <Divider />

              <Stack spacing={2}>
                <Typography variant="h6">Renk</Typography>
                <ToggleButtonGroup
                  exclusive
                  value={selectedColor}
                  onChange={(_, value) => {
                    if (value) {
                      setSelectedColor(value);
                    }
                  }}
                  sx={{ flexWrap: 'wrap', gap: 1 }}
                >
                  {product.colors.map((color) => (
                    <ToggleButton
                      key={color.name}
                      value={color.name}
                      sx={{ gap: 1, borderRadius: '999px !important', px: 1.5 }}
                    >
                      <Box
                        sx={{
                          width: 18,
                          height: 18,
                          borderRadius: '50%',
                          bgcolor: color.value,
                          border: 1,
                          borderColor: 'divider',
                        }}
                      />
                      {color.name}
                    </ToggleButton>
                  ))}
                </ToggleButtonGroup>
              </Stack>

              <Stack spacing={2}>
                <Typography variant="h6">Beden</Typography>
                <ToggleButtonGroup
                  exclusive
                  value={selectedSize}
                  onChange={(_, value) => {
                    if (value) {
                      setSelectedSize(value);
                    }
                  }}
                  sx={{ flexWrap: 'wrap', gap: 1 }}
                >
                  {product.sizes.map((size) => (
                    <ToggleButton
                      key={size}
                      value={size}
                      sx={{
                        minWidth: 54,
                        borderRadius: '999px !important',
                        px: 2,
                      }}
                    >
                      {size}
                    </ToggleButton>
                  ))}
                </ToggleButtonGroup>
              </Stack>

              <Stack direction={{ xs: 'column', sm: 'row' }} spacing={1.5}>
                <QuantitySelector value={quantity} onChange={setQuantity} max={product.stock} />
                <Button
                  variant="contained"
                  size="large"
                  startIcon={<AddShoppingCartIcon />}
                  onClick={() => setSnackbarOpen(true)}
                  fullWidth
                >
                  Sepete Ekle
                </Button>
                <Button variant="outlined" size="large" startIcon={<FavoriteBorderIcon />}>
                  Favori
                </Button>
              </Stack>

              <Box>
                <Tabs value={tab} onChange={(_, value) => setTab(value)} variant="scrollable">
                  <Tab label="Ürün Detayı" />
                  <Tab label="Kumaş ve Bakım" />
                  <Tab label="Teslimat" />
                </Tabs>
                <Box sx={{ pt: 2.5 }}>
                  {tab === 0 ? (
                    <Stack component="ul" spacing={1} sx={{ pl: 2.5, m: 0 }}>
                      {product.details.map((detail) => (
                        <Typography component="li" color="text.secondary" key={detail}>
                          {detail}
                        </Typography>
                      ))}
                    </Stack>
                  ) : null}
                  {tab === 1 ? (
                    <Typography color="text.secondary">
                      Hassas dokular için düşük ısıda ütü ve nazik yıkama önerilir. Ürün
                      bakım talimatları gerçek katalog entegrasyonunda backend verisiyle
                      güncellenecek.
                    </Typography>
                  ) : null}
                  {tab === 2 ? (
                    <Typography color="text.secondary">
                      Kargo ve iade akışları checkout entegrasyonu sırasında gerçek servis
                      kurallarına bağlanacak.
                    </Typography>
                  ) : null}
                </Box>
              </Box>
            </Stack>
          </Grid>
        </Grid>

        {relatedProducts.length ? (
          <Box>
            <SectionHeader
              eyebrow="Benzer Ürünler"
              title="Bu stile yakın parçalar"
              actionLabel="Tümünü Gör"
              actionHref={`/products?category=${product.category}`}
            />
            <Grid container spacing={{ xs: 2, md: 3 }}>
              {relatedProducts.map((relatedProduct) => (
                <Grid item xs={6} md={3} key={relatedProduct.id}>
                  <ProductCard product={relatedProduct} />
                </Grid>
              ))}
            </Grid>
          </Box>
        ) : null}
      </Stack>

      <Snackbar
        open={snackbarOpen}
        autoHideDuration={2600}
        onClose={() => setSnackbarOpen(false)}
        anchorOrigin={{ vertical: 'bottom', horizontal: 'center' }}
      >
        <Alert severity="success" variant="filled" onClose={() => setSnackbarOpen(false)}>
          Ürün sepete eklenmek üzere hazırlandı.
        </Alert>
      </Snackbar>
    </PageContainer>
  );
}

