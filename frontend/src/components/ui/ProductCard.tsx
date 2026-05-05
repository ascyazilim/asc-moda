import FavoriteBorderIcon from '@mui/icons-material/FavoriteBorder';
import ShoppingBagOutlinedIcon from '@mui/icons-material/ShoppingBagOutlined';
import {
  Box,
  Button,
  Card,
  CardActionArea,
  CardContent,
  Chip,
  IconButton,
  Stack,
  Typography,
} from '@mui/material';
import { Link as RouterLink } from 'react-router-dom';

import { Product } from '../../types/product';
import { PriceDisplay } from './PriceDisplay';

type ProductCardProps = {
  product: Product;
};

export function ProductCard({ product }: ProductCardProps) {
  return (
    <Card
      sx={{
        height: '100%',
        overflow: 'hidden',
        transition: 'transform 180ms ease, border-color 180ms ease',
        '&:hover': {
          transform: { md: 'translateY(-4px)' },
          borderColor: 'secondary.main',
        },
      }}
    >
      <Box sx={{ position: 'relative' }}>
        <CardActionArea component={RouterLink} to={`/products/${product.slug}`}>
          <Box
            component="img"
            src={product.images[0]}
            alt={product.name}
            loading="lazy"
            sx={{
              width: '100%',
              aspectRatio: '4 / 5',
              objectFit: 'cover',
              bgcolor: 'secondary.light',
            }}
          />
        </CardActionArea>
        <Stack direction="row" spacing={1} sx={{ position: 'absolute', top: 12, left: 12 }}>
          {product.isNew ? <Chip label="Yeni" size="small" color="secondary" /> : null}
          {product.compareAtPrice ? <Chip label="Seçili" size="small" /> : null}
        </Stack>
        <IconButton
          aria-label={`${product.name} favorilere ekle`}
          size="small"
          sx={{
            position: 'absolute',
            top: 10,
            right: 10,
            bgcolor: 'rgba(255,255,255,0.86)',
            '&:hover': {
              bgcolor: 'background.paper',
            },
          }}
        >
          <FavoriteBorderIcon fontSize="small" />
        </IconButton>
      </Box>
      <CardContent sx={{ p: { xs: 1.5, sm: 2 } }}>
        <Stack spacing={1.25}>
          <Typography variant="body2" color="text.secondary" fontWeight={600}>
            {product.categoryLabel}
          </Typography>
          <Typography
            component={RouterLink}
            to={`/products/${product.slug}`}
            variant="h6"
            sx={{
              minHeight: { xs: 52, sm: 58 },
              display: '-webkit-box',
              overflow: 'hidden',
              WebkitBoxOrient: 'vertical',
              WebkitLineClamp: 2,
            }}
          >
            {product.name}
          </Typography>
          <PriceDisplay
            price={product.price}
            maxPrice={product.maxPrice}
            compareAtPrice={product.compareAtPrice}
          />
          <Button
            component={RouterLink}
            to={`/products/${product.slug}`}
            variant="outlined"
            size="small"
            startIcon={<ShoppingBagOutlinedIcon />}
            sx={{ alignSelf: 'flex-start' }}
          >
            İncele
          </Button>
        </Stack>
      </CardContent>
    </Card>
  );
}
