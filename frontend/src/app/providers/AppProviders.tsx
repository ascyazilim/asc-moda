import { PropsWithChildren, useState } from 'react';
import { CssBaseline, GlobalStyles, ThemeProvider } from '@mui/material';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';

import { storefrontTheme } from '../theme/theme';

export function AppProviders({ children }: PropsWithChildren) {
  const [queryClient] = useState(
    () =>
      new QueryClient({
        defaultOptions: {
          queries: {
            staleTime: 1000 * 60 * 5,
            refetchOnWindowFocus: false,
            retry: 1,
          },
        },
      }),
  );

  return (
    <QueryClientProvider client={queryClient}>
      <ThemeProvider theme={storefrontTheme}>
        <CssBaseline />
        <GlobalStyles
          styles={(theme) => ({
            html: {
              scrollBehavior: 'smooth',
            },
            body: {
              minWidth: 320,
              background: theme.palette.background.default,
            },
            a: {
              color: 'inherit',
              textDecoration: 'none',
            },
            img: {
              display: 'block',
              maxWidth: '100%',
            },
            '::selection': {
              backgroundColor: theme.palette.secondary.light,
              color: theme.palette.primary.dark,
            },
          })}
        />
        {children}
      </ThemeProvider>
    </QueryClientProvider>
  );
}

