import AccountCircleOutlinedIcon from '@mui/icons-material/AccountCircleOutlined';
import CloseIcon from '@mui/icons-material/Close';
import FavoriteBorderIcon from '@mui/icons-material/FavoriteBorder';
import InstagramIcon from '@mui/icons-material/Instagram';
import MenuIcon from '@mui/icons-material/Menu';
import PinterestIcon from '@mui/icons-material/Pinterest';
import SearchIcon from '@mui/icons-material/Search';
import ShoppingBagOutlinedIcon from '@mui/icons-material/ShoppingBagOutlined';
import {
  AppBar,
  Badge,
  Box,
  Button,
  Container,
  Divider,
  Drawer,
  IconButton,
  Link,
  Stack,
  Toolbar,
  Typography,
} from '@mui/material';
import { useState } from 'react';
import { Link as RouterLink, Outlet, useNavigate } from 'react-router-dom';

import { SearchInput } from '../../components/ui/SearchInput';
import { ScrollToTop } from '../router/ScrollToTop';

const navItems = [
  { label: 'Yeni Sezon', href: '/products?sort=newest' },
  { label: 'Başörtü', href: '/products?category=basortu' },
  { label: 'Şal', href: '/products?category=sal' },
  { label: 'Elbise', href: '/products?category=elbise' },
  { label: 'Etek', href: '/products?category=etek' },
  { label: 'Bluz', href: '/products?category=bluz' },
  { label: 'Dış Giyim', href: '/products?category=dis-giyim' },
];

export function StorefrontLayout() {
  const [mobileOpen, setMobileOpen] = useState(false);
  const [searchTerm, setSearchTerm] = useState('');
  const navigate = useNavigate();

  const handleSearch = () => {
    const query = searchTerm.trim();
    navigate(query ? `/search?q=${encodeURIComponent(query)}` : '/search');
    setMobileOpen(false);
  };

  const navigation = (
    <Stack
      direction={{ xs: 'column', lg: 'row' }}
      spacing={{ xs: 0.5, lg: 0.25 }}
      alignItems={{ xs: 'stretch', lg: 'center' }}
    >
      {navItems.map((item) => (
        <Button
          key={item.href}
          component={RouterLink}
          to={item.href}
          color="inherit"
          onClick={() => setMobileOpen(false)}
          sx={{
            justifyContent: { xs: 'flex-start', lg: 'center' },
            px: { xs: 1.5, lg: 1.4 },
            color: 'text.primary',
            fontWeight: 600,
          }}
        >
          {item.label}
        </Button>
      ))}
    </Stack>
  );

  return (
    <Box sx={{ minHeight: '100vh', display: 'flex', flexDirection: 'column' }}>
      <ScrollToTop />
      <AppBar
        position="sticky"
        color="inherit"
        elevation={0}
        sx={{
          borderBottom: 1,
          borderColor: 'divider',
          bgcolor: 'rgba(251, 248, 242, 0.92)',
          backdropFilter: 'blur(18px)',
        }}
      >
        <Container maxWidth="xl">
          <Toolbar disableGutters sx={{ minHeight: { xs: 68, md: 78 }, gap: 1.5 }}>
            <IconButton
              aria-label="Menüyü aç"
              onClick={() => setMobileOpen(true)}
              sx={{ display: { xs: 'inline-flex', lg: 'none' } }}
            >
              <MenuIcon />
            </IconButton>

            <Typography
              component={RouterLink}
              to="/"
              variant="h4"
              sx={{
                fontFamily: "Georgia, 'Times New Roman', serif",
                fontWeight: 500,
                whiteSpace: 'nowrap',
                mr: { lg: 2 },
              }}
            >
              Asc Moda
            </Typography>

            <Box sx={{ display: { xs: 'none', lg: 'block' }, flex: 1 }}>{navigation}</Box>

            <Stack
              direction="row"
              spacing={0.5}
              alignItems="center"
              sx={{ ml: 'auto' }}
            >
              <Box sx={{ width: 280, display: { xs: 'none', md: 'block', lg: 'none', xl: 'block' } }}>
                <SearchInput
                  size="small"
                  value={searchTerm}
                  onChange={(event) => setSearchTerm(event.target.value)}
                  onKeyDown={(event) => {
                    if (event.key === 'Enter') {
                      handleSearch();
                    }
                  }}
                  onSearch={handleSearch}
                />
              </Box>
              <IconButton
                aria-label="Arama"
                component={RouterLink}
                to="/search"
                sx={{ display: { md: 'none' } }}
              >
                <SearchIcon />
              </IconButton>
              <IconButton aria-label="Favoriler">
                <FavoriteBorderIcon />
              </IconButton>
              <IconButton aria-label="Hesabım">
                <AccountCircleOutlinedIcon />
              </IconButton>
              <IconButton aria-label="Sepet" component={RouterLink} to="/cart">
                <Badge badgeContent={2} color="secondary">
                  <ShoppingBagOutlinedIcon />
                </Badge>
              </IconButton>
            </Stack>
          </Toolbar>
        </Container>
      </AppBar>

      <Drawer
        open={mobileOpen}
        onClose={() => setMobileOpen(false)}
        PaperProps={{
          sx: {
            width: 'min(88vw, 380px)',
            bgcolor: 'background.default',
          },
        }}
      >
        <Stack spacing={2.5} sx={{ p: 2.5, minHeight: '100%' }}>
          <Stack direction="row" justifyContent="space-between" alignItems="center">
            <Typography
              variant="h4"
              sx={{ fontFamily: "Georgia, 'Times New Roman', serif", fontWeight: 500 }}
            >
              Asc Moda
            </Typography>
            <IconButton aria-label="Menüyü kapat" onClick={() => setMobileOpen(false)}>
              <CloseIcon />
            </IconButton>
          </Stack>
          <SearchInput
            size="small"
            value={searchTerm}
            onChange={(event) => setSearchTerm(event.target.value)}
            onKeyDown={(event) => {
              if (event.key === 'Enter') {
                handleSearch();
              }
            }}
            onSearch={handleSearch}
          />
          <Divider />
          {navigation}
          <Box sx={{ mt: 'auto' }}>
            <Typography variant="body2" color="text.secondary">
              Premium tesettür giyim için sade, zarif ve zamansız parçalar.
            </Typography>
          </Box>
        </Stack>
      </Drawer>

      <Box component="main" sx={{ flex: 1 }}>
        <Outlet />
      </Box>

      <Footer />
    </Box>
  );
}

function Footer() {
  const categoryLinks = navItems.slice(1);

  return (
    <Box component="footer" sx={{ bgcolor: 'primary.dark', color: '#fffaf2', mt: { xs: 4, md: 7 } }}>
      <Container maxWidth="xl" sx={{ py: { xs: 5, md: 7 } }}>
        <Box
          sx={{
            display: 'grid',
            gridTemplateColumns: {
              xs: '1fr',
              sm: '1.2fr 1fr',
              md: '1.4fr 1fr 1fr 1fr',
            },
            gap: { xs: 4, md: 6 },
          }}
        >
          <Stack spacing={2}>
            <Typography
              variant="h3"
              sx={{ fontFamily: "Georgia, 'Times New Roman', serif", fontWeight: 500 }}
            >
              Asc Moda
            </Typography>
            <Typography sx={{ color: 'rgba(255,250,242,0.72)', maxWidth: 420 }}>
              Başörtüden dış giyime uzanan seçkisiyle sakin, ferah ve premium bir
              tesettür giyim deneyimi.
            </Typography>
            <Stack direction="row" spacing={1}>
              <IconButton aria-label="Instagram" sx={{ color: 'inherit', border: 1, borderColor: 'rgba(255,255,255,0.18)' }}>
                <InstagramIcon />
              </IconButton>
              <IconButton aria-label="Pinterest" sx={{ color: 'inherit', border: 1, borderColor: 'rgba(255,255,255,0.18)' }}>
                <PinterestIcon />
              </IconButton>
            </Stack>
          </Stack>

          <FooterColumn title="Kategoriler">
            {categoryLinks.map((item) => (
              <Link key={item.href} component={RouterLink} to={item.href} color="inherit">
                {item.label}
              </Link>
            ))}
          </FooterColumn>

          <FooterColumn title="Yardım">
            <Link color="inherit">Kargo ve Teslimat</Link>
            <Link color="inherit">Kolay İade</Link>
            <Link color="inherit">Beden Rehberi</Link>
            <Link color="inherit">Sık Sorulanlar</Link>
          </FooterColumn>

          <FooterColumn title="İletişim">
            <Typography variant="body2">hello@ascmoda.com</Typography>
            <Typography variant="body2">+90 212 000 00 00</Typography>
            <Typography variant="body2">İstanbul, Türkiye</Typography>
          </FooterColumn>
        </Box>

        <Divider sx={{ borderColor: 'rgba(255,255,255,0.14)', my: { xs: 4, md: 5 } }} />

        <Stack
          direction={{ xs: 'column', sm: 'row' }}
          justifyContent="space-between"
          spacing={1.5}
          sx={{ color: 'rgba(255,250,242,0.64)' }}
        >
          <Typography variant="body2">© 2026 Asc Moda. Tüm hakları saklıdır.</Typography>
          <Typography variant="body2">Güvenli ödeme altyapısı için hazır storefront temeli.</Typography>
        </Stack>
      </Container>
    </Box>
  );
}

type FooterColumnProps = {
  title: string;
  children: React.ReactNode;
};

function FooterColumn({ title, children }: FooterColumnProps) {
  return (
    <Stack spacing={1.25} sx={{ color: 'rgba(255,250,242,0.72)' }}>
      <Typography variant="h6" color="#fffaf2">
        {title}
      </Typography>
      {children}
    </Stack>
  );
}
