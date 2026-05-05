import { Box, Stack, Typography } from '@mui/material';

type PageHeroProps = {
  eyebrow?: string;
  title: string;
  description?: string;
};

export function PageHero({ eyebrow, title, description }: PageHeroProps) {
  return (
    <Box
      sx={{
        borderBottom: 1,
        borderColor: 'divider',
        bgcolor: 'background.default',
        py: { xs: 4, md: 7 },
      }}
    >
      <Stack spacing={1.5} sx={{ maxWidth: 840 }}>
        {eyebrow ? (
          <Typography
            variant="overline"
            color="secondary.dark"
            sx={{ fontWeight: 700, letterSpacing: 1.4 }}
          >
            {eyebrow}
          </Typography>
        ) : null}
        <Typography variant="h1">{title}</Typography>
        {description ? (
          <Typography color="text.secondary" sx={{ maxWidth: 680 }}>
            {description}
          </Typography>
        ) : null}
      </Stack>
    </Box>
  );
}

