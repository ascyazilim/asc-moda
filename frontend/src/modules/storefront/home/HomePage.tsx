import ArrowForwardIcon from '@mui/icons-material/ArrowForward';
import LocalShippingOutlinedIcon from '@mui/icons-material/LocalShippingOutlined';
import LockOutlinedIcon from '@mui/icons-material/LockOutlined';
import ReplayOutlinedIcon from '@mui/icons-material/ReplayOutlined';
import SpaOutlinedIcon from '@mui/icons-material/SpaOutlined';
import {
  Box,
  Button,
  CircularProgress,
  Grid,
  Paper,
  Stack,
  TextField,
  Typography,
} from '@mui/material';
import { zodResolver } from '@hookform/resolvers/zod';
import { useForm } from 'react-hook-form';
import { Link as RouterLink } from 'react-router-dom';
import { z } from 'zod';

import { PageContainer } from '../../../components/common/PageContainer';
import { SectionHeader } from '../../../components/common/SectionHeader';
import { CategoryCard } from '../../../components/ui/CategoryCard';
import { ProductCard } from '../../../components/ui/ProductCard';
import {
  useCategories,
  useFeaturedProducts,
  useNewArrivals,
} from '../../../hooks/useStorefrontQueries';
import { Product } from '../../../types/product';
import {
  heroImage,
  storefrontCategories,
} from '../mock/storefrontData';

const newsletterSchema = z.object({
  email: z.string().email('Geçerli bir e-posta adresi girin.'),
});

type NewsletterFormValues = z.infer<typeof newsletterSchema>;

export function HomePage() {
  const categoriesQuery = useCategories();
  const featuredQuery = useFeaturedProducts();
  const newArrivalsQuery = useNewArrivals();
  const categories = categoriesQuery.data?.length ? categoriesQuery.data : storefrontCategories;

  return (
    <>
      <HeroSection />

      <PageContainer>
        <SectionHeader
          eyebrow="Kategoriler"
          title="Sezonun sakin ve zarif parçaları"
          description="Başörtü, şal, elbise, etek ve dış giyim seçkisi butik bir ritimle bir araya geldi."
        />
        <Grid container spacing={{ xs: 2, md: 3 }}>
          {categories.map((category) => (
            <Grid item xs={12} sm={6} lg={4} key={category.slug}>
              <CategoryCard category={category} />
            </Grid>
          ))}
        </Grid>
      </PageContainer>

      <PageContainer>
        <SectionHeader
          eyebrow="Öne Çıkanlar"
          title="Asc Moda seçkisi"
          description="Premium dokular, yumuşak renk paleti ve günlük şıklığı yükselten formlar."
          actionLabel="Tüm Ürünler"
          actionHref="/products"
        />
        {featuredQuery.isLoading ? (
          <CenteredLoader />
        ) : (
          <ProductGrid products={featuredQuery.data ?? []} />
        )}
      </PageContainer>

      <CollectionBanner />

      <PageContainer>
        <SectionHeader
          eyebrow="Yeni Gelenler"
          title="Gardıroba taze bir nefes"
          description="Yeni sezonun hafif dokuları ve premium nötr tonları."
          actionLabel="Yeni Sezonu Gör"
          actionHref="/products?sort=newest"
        />
        {newArrivalsQuery.isLoading ? (
          <CenteredLoader />
        ) : (
          <ProductGrid products={newArrivalsQuery.data ?? []} />
        )}
      </PageContainer>

      <TrustSection />
      <NewsletterSection />
    </>
  );
}

function HeroSection() {
  return (
    <Box
      sx={{
        borderBottom: 1,
        borderColor: 'divider',
        bgcolor: 'background.default',
      }}
    >
      <PageContainer sx={{ py: { xs: 4, md: 8, lg: 10 } }}>
        <Box
          sx={{
            display: 'grid',
            gridTemplateColumns: { xs: '1fr', lg: '0.9fr 1.1fr' },
            gap: { xs: 4, md: 6 },
            alignItems: 'center',
          }}
        >
          <Stack spacing={{ xs: 2.5, md: 3.5 }} sx={{ maxWidth: 620 }}>
            <Typography
              variant="overline"
              color="secondary.dark"
              sx={{ fontWeight: 800, letterSpacing: 1.5 }}
            >
              Premium Tesettür Giyim
            </Typography>
            <Typography variant="h1">Sakin lüksün zarif hali</Typography>
            <Typography color="text.secondary" sx={{ fontSize: { xs: 16, md: 18 } }}>
              Asc Moda; başörtü, şal, elbise ve dış giyim parçalarını ferah,
              zamansız ve butik bir vitrin deneyimiyle buluşturur.
            </Typography>
            <Stack direction={{ xs: 'column', sm: 'row' }} spacing={1.5}>
              <Button
                component={RouterLink}
                to="/products"
                variant="contained"
                size="large"
                endIcon={<ArrowForwardIcon />}
              >
                Koleksiyonu Keşfet
              </Button>
              <Button
                component={RouterLink}
                to="/products?sort=newest"
                variant="outlined"
                size="large"
              >
                Yeni Gelenler
              </Button>
            </Stack>
          </Stack>

          <Box
            sx={{
              position: 'relative',
              minHeight: { xs: 360, sm: 460, lg: 620 },
              overflow: 'hidden',
              borderRadius: 2,
            }}
          >
            <Box
              component="img"
              src={heroImage}
              alt="Asc Moda premium tesettür koleksiyonu"
              sx={{
                width: '100%',
                height: '100%',
                minHeight: { xs: 360, sm: 460, lg: 620 },
                objectFit: 'cover',
                filter: 'saturate(0.88)',
              }}
            />
            <Box
              sx={{
                position: 'absolute',
                inset: 0,
                background:
                  'linear-gradient(180deg, rgba(251,248,242,0.02) 0%, rgba(47,37,33,0.22) 100%)',
              }}
            />
          </Box>
        </Box>
      </PageContainer>
    </Box>
  );
}

function ProductGrid({ products }: { products: Product[] }) {
  return (
    <Grid container spacing={{ xs: 2, md: 3 }}>
      {products.map((product) => (
        <Grid item xs={6} md={3} key={product.id}>
          <ProductCard product={product} />
        </Grid>
      ))}
    </Grid>
  );
}

function CollectionBanner() {
  return (
    <PageContainer sx={{ py: { xs: 3, md: 5 } }}>
      <Box
        sx={{
          display: 'grid',
          gridTemplateColumns: { xs: '1fr', md: '1fr 1fr' },
          bgcolor: 'secondary.light',
          border: 1,
          borderColor: 'divider',
          overflow: 'hidden',
        }}
      >
        <Stack spacing={2.5} sx={{ p: { xs: 3, md: 6 }, justifyContent: 'center' }}>
          <Typography variant="overline" color="secondary.dark" sx={{ fontWeight: 800 }}>
            Koleksiyon
          </Typography>
          <Typography variant="h2">İvory edit: ferah kombinlerin ana tonu</Typography>
          <Typography color="text.secondary">
            Şal, tunik ve dış giyim parçalarında ivory, vizon ve taupe tonlarını
            aynı çizgide buluşturan sade bir sezon anlatısı.
          </Typography>
          <Button
            component={RouterLink}
            to="/products?category=sal"
            variant="contained"
            sx={{ alignSelf: 'flex-start' }}
          >
            Editi İncele
          </Button>
        </Stack>
        <Box
          component="img"
          src="https://images.unsplash.com/photo-1523381210434-271e8be1f52b?auto=format&fit=crop&w=1000&q=85"
          alt="Ivory ve nötr tonlarda kumaş koleksiyonu"
          sx={{
            width: '100%',
            height: '100%',
            minHeight: { xs: 280, md: 430 },
            objectFit: 'cover',
          }}
        />
      </Box>
    </PageContainer>
  );
}

function TrustSection() {
  const items = [
    {
      icon: <LockOutlinedIcon />,
      title: 'Güvenli Ödeme',
      description: 'Ödeme akışı için güvenli entegrasyon yapısına hazır.',
    },
    {
      icon: <LocalShippingOutlinedIcon />,
      title: 'Hızlı Kargo',
      description: 'Sipariş sonrası süreçler net ve izlenebilir olacak şekilde kurgulandı.',
    },
    {
      icon: <ReplayOutlinedIcon />,
      title: 'Kolay İade',
      description: 'Müşteri deneyimini sade tutan iade ve değişim alanları.',
    },
    {
      icon: <SpaOutlinedIcon />,
      title: 'Seçili Kumaşlar',
      description: 'Butik marka hissini destekleyen premium ürün sunumu.',
    },
  ];

  return (
    <PageContainer>
      <Grid container spacing={2}>
        {items.map((item) => (
          <Grid item xs={12} sm={6} lg={3} key={item.title}>
            <Paper
              elevation={0}
              sx={{
                height: '100%',
                p: 3,
                border: 1,
                borderColor: 'divider',
                bgcolor: 'background.paper',
              }}
            >
              <Stack spacing={1.5}>
                <Box sx={{ color: 'secondary.dark' }}>{item.icon}</Box>
                <Typography variant="h6">{item.title}</Typography>
                <Typography variant="body2" color="text.secondary">
                  {item.description}
                </Typography>
              </Stack>
            </Paper>
          </Grid>
        ))}
      </Grid>
    </PageContainer>
  );
}

function NewsletterSection() {
  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitSuccessful },
  } = useForm<NewsletterFormValues>({
    resolver: zodResolver(newsletterSchema),
    defaultValues: {
      email: '',
    },
  });

  return (
    <PageContainer sx={{ pb: { xs: 2, md: 4 } }}>
      <Paper
        elevation={0}
        sx={{
          border: 1,
          borderColor: 'divider',
          bgcolor: 'background.paper',
          p: { xs: 3, md: 5 },
        }}
      >
        <Box
          component="form"
          onSubmit={handleSubmit(() => undefined)}
          sx={{
            display: 'grid',
            gridTemplateColumns: { xs: '1fr', md: '1fr minmax(320px, 0.7fr)' },
            gap: { xs: 3, md: 5 },
            alignItems: 'center',
          }}
        >
          <Stack spacing={1.5}>
            <Typography variant="overline" color="secondary.dark" sx={{ fontWeight: 800 }}>
              E-bülten
            </Typography>
            <Typography variant="h3">Yeni koleksiyonlardan ilk siz haberdar olun</Typography>
            <Typography color="text.secondary">
              Kampanya kalabalığı yerine seçili ürün haberleri ve stil ilhamları.
            </Typography>
          </Stack>

          <Stack direction={{ xs: 'column', sm: 'row' }} spacing={1.5}>
            <TextField
              {...register('email')}
              label="E-posta adresiniz"
              error={Boolean(errors.email)}
              helperText={
                errors.email?.message ??
                (isSubmitSuccessful ? 'Kaydınız alındı. Teşekkür ederiz.' : ' ')
              }
              sx={{ flex: 1 }}
            />
            <Button type="submit" variant="contained" sx={{ alignSelf: { sm: 'flex-start' } }}>
              Kaydol
            </Button>
          </Stack>
        </Box>
      </Paper>
    </PageContainer>
  );
}

function CenteredLoader() {
  return (
    <Stack alignItems="center" sx={{ py: 8 }}>
      <CircularProgress color="secondary" />
    </Stack>
  );
}
