import { alpha, createTheme } from '@mui/material/styles';

const palette = {
  ivory: '#fbf8f2',
  porcelain: '#fffdf8',
  warmSand: '#eadfce',
  mist: '#d8c8b6',
  taupe: '#9c7e63',
  moka: '#4a352d',
  espresso: '#2f2521',
  sage: '#8e9a84',
  blush: '#c5a39b',
};

export const storefrontTheme = createTheme({
  palette: {
    mode: 'light',
    primary: {
      main: palette.moka,
      light: '#7a5c4c',
      dark: palette.espresso,
      contrastText: '#fffaf2',
    },
    secondary: {
      main: palette.taupe,
      light: palette.warmSand,
      dark: '#715845',
      contrastText: palette.espresso,
    },
    success: {
      main: palette.sage,
    },
    background: {
      default: palette.ivory,
      paper: palette.porcelain,
    },
    text: {
      primary: palette.espresso,
      secondary: '#76665c',
    },
    divider: alpha(palette.moka, 0.12),
  },
  shape: {
    borderRadius: 8,
  },
  typography: {
    fontFamily:
      "'Inter', 'Avenir Next', 'Segoe UI', 'Helvetica Neue', Arial, sans-serif",
    h1: {
      fontFamily: "Georgia, 'Times New Roman', serif",
      fontWeight: 500,
      lineHeight: 1.05,
      letterSpacing: 0,
      fontSize: 'clamp(2.55rem, 6vw, 5.75rem)',
    },
    h2: {
      fontFamily: "Georgia, 'Times New Roman', serif",
      fontWeight: 500,
      lineHeight: 1.12,
      letterSpacing: 0,
      fontSize: 'clamp(2rem, 3.8vw, 3.75rem)',
    },
    h3: {
      fontFamily: "Georgia, 'Times New Roman', serif",
      fontWeight: 500,
      lineHeight: 1.18,
      letterSpacing: 0,
      fontSize: 'clamp(1.55rem, 2.6vw, 2.5rem)',
    },
    h4: {
      fontWeight: 600,
      lineHeight: 1.24,
      letterSpacing: 0,
      fontSize: 'clamp(1.25rem, 2vw, 1.875rem)',
    },
    h5: {
      fontWeight: 600,
      lineHeight: 1.3,
      letterSpacing: 0,
    },
    h6: {
      fontWeight: 600,
      letterSpacing: 0,
    },
    button: {
      textTransform: 'none',
      fontWeight: 600,
      letterSpacing: 0,
    },
    body1: {
      lineHeight: 1.7,
    },
    body2: {
      lineHeight: 1.6,
    },
  },
  components: {
    MuiButton: {
      defaultProps: {
        disableElevation: true,
      },
      styleOverrides: {
        root: {
          borderRadius: 999,
          minHeight: 44,
          paddingInline: 22,
        },
        containedPrimary: {
          backgroundColor: palette.moka,
          '&:hover': {
            backgroundColor: palette.espresso,
          },
        },
        outlinedPrimary: {
          borderColor: alpha(palette.moka, 0.32),
          '&:hover': {
            borderColor: palette.moka,
            backgroundColor: alpha(palette.moka, 0.06),
          },
        },
      },
    },
    MuiCard: {
      styleOverrides: {
        root: {
          borderRadius: 8,
          boxShadow: 'none',
          border: `1px solid ${alpha(palette.moka, 0.11)}`,
          backgroundImage: 'none',
        },
      },
    },
    MuiPaper: {
      styleOverrides: {
        root: {
          backgroundImage: 'none',
        },
      },
    },
    MuiChip: {
      styleOverrides: {
        root: {
          borderRadius: 999,
          fontWeight: 600,
        },
      },
    },
    MuiTextField: {
      defaultProps: {
        variant: 'outlined',
      },
    },
    MuiOutlinedInput: {
      styleOverrides: {
        root: {
          borderRadius: 8,
          backgroundColor: palette.porcelain,
          '& fieldset': {
            borderColor: alpha(palette.moka, 0.16),
          },
          '&:hover fieldset': {
            borderColor: alpha(palette.moka, 0.38),
          },
          '&.Mui-focused fieldset': {
            borderColor: palette.taupe,
            borderWidth: 1,
          },
        },
      },
    },
    MuiContainer: {
      defaultProps: {
        maxWidth: 'xl',
      },
      styleOverrides: {
        root: ({ theme }) => ({
          paddingLeft: theme.spacing(2),
          paddingRight: theme.spacing(2),
          [theme.breakpoints.up('sm')]: {
            paddingLeft: theme.spacing(3),
            paddingRight: theme.spacing(3),
          },
          [theme.breakpoints.up('lg')]: {
            paddingLeft: theme.spacing(5),
            paddingRight: theme.spacing(5),
          },
        }),
      },
    },
  },
});

