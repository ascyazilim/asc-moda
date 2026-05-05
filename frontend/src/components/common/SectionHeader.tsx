import { Box, Button, Stack, Typography } from '@mui/material';
import { Link as RouterLink } from 'react-router-dom';

type SectionHeaderProps = {
  eyebrow?: string;
  title: string;
  description?: string;
  actionLabel?: string;
  actionHref?: string;
};

export function SectionHeader({
  eyebrow,
  title,
  description,
  actionLabel,
  actionHref,
}: SectionHeaderProps) {
  return (
    <Stack
      direction={{ xs: 'column', md: 'row' }}
      alignItems={{ xs: 'flex-start', md: 'flex-end' }}
      justifyContent="space-between"
      spacing={2}
      sx={{ mb: { xs: 2.5, md: 4 } }}
    >
      <Box sx={{ maxWidth: 720 }}>
        {eyebrow ? (
          <Typography
            variant="overline"
            color="secondary.dark"
            sx={{ fontWeight: 700, letterSpacing: 1.4 }}
          >
            {eyebrow}
          </Typography>
        ) : null}
        <Typography variant="h2" sx={{ mt: eyebrow ? 0.5 : 0 }}>
          {title}
        </Typography>
        {description ? (
          <Typography color="text.secondary" sx={{ mt: 1.5, maxWidth: 640 }}>
            {description}
          </Typography>
        ) : null}
      </Box>

      {actionLabel && actionHref ? (
        <Button component={RouterLink} to={actionHref} variant="outlined">
          {actionLabel}
        </Button>
      ) : null}
    </Stack>
  );
}

