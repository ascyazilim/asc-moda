import { Box, Button, Stack, Typography } from '@mui/material';
import { Link as RouterLink } from 'react-router-dom';

type EmptyStateProps = {
  title: string;
  description?: string;
  actionLabel?: string;
  actionHref?: string;
};

export function EmptyState({ title, description, actionLabel, actionHref }: EmptyStateProps) {
  return (
    <Box
      sx={{
        border: 1,
        borderColor: 'divider',
        bgcolor: 'background.paper',
        px: { xs: 2.5, sm: 4 },
        py: { xs: 5, sm: 7 },
        textAlign: 'center',
      }}
    >
      <Stack alignItems="center" spacing={2}>
        <Typography variant="h4">{title}</Typography>
        {description ? (
          <Typography color="text.secondary" sx={{ maxWidth: 520 }}>
            {description}
          </Typography>
        ) : null}
        {actionLabel && actionHref ? (
          <Button component={RouterLink} to={actionHref} variant="contained">
            {actionLabel}
          </Button>
        ) : null}
      </Stack>
    </Box>
  );
}

