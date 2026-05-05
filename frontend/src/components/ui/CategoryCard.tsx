import ArrowForwardIcon from '@mui/icons-material/ArrowForward';
import { Box, Card, CardActionArea, Stack, Typography } from '@mui/material';
import { Link as RouterLink } from 'react-router-dom';

import { Category } from '../../types/product';

type CategoryCardProps = {
  category: Category;
};

export function CategoryCard({ category }: CategoryCardProps) {
  return (
    <Card sx={{ overflow: 'hidden', height: '100%' }}>
      <CardActionArea component={RouterLink} to={category.href} sx={{ height: '100%' }}>
        <Box sx={{ position: 'relative', minHeight: { xs: 220, md: 280 } }}>
          <Box
            component="img"
            src={category.image}
            alt={category.title}
            loading="lazy"
            sx={{
              width: '100%',
              height: '100%',
              minHeight: { xs: 220, md: 280 },
              objectFit: 'cover',
              filter: 'saturate(0.86)',
            }}
          />
          <Box
            sx={{
              position: 'absolute',
              inset: 0,
              background:
                'linear-gradient(180deg, rgba(47,37,33,0.06) 0%, rgba(47,37,33,0.66) 100%)',
            }}
          />
          <Stack
            spacing={1}
            sx={{
              position: 'absolute',
              insetInline: 18,
              bottom: 18,
              color: '#fffaf2',
            }}
          >
            <Stack direction="row" alignItems="center" justifyContent="space-between" spacing={2}>
              <Typography variant="h4">{category.title}</Typography>
              <ArrowForwardIcon />
            </Stack>
            <Typography variant="body2" sx={{ maxWidth: 320 }}>
              {category.description}
            </Typography>
          </Stack>
        </Box>
      </CardActionArea>
    </Card>
  );
}

