import CheckCircleOutlineIcon from '@mui/icons-material/CheckCircleOutline';
import ErrorOutlineIcon from '@mui/icons-material/ErrorOutline';
import {
  Alert,
  Box,
  Button,
  Checkbox,
  Chip,
  CircularProgress,
  FormControlLabel,
  Paper,
  Snackbar,
  Stack,
  TextField,
  Typography,
} from '@mui/material';
import { zodResolver } from '@hookform/resolvers/zod';
import { useEffect, useState } from 'react';
import { useForm } from 'react-hook-form';
import { z } from 'zod';

import { useCurrentCustomer, useUpdateCustomerProfileMutation } from '../../../hooks/useAccountQueries';
import { normalizeApiError } from '../../../services/api/client';

const profileSchema = z.object({
  firstName: z.string().min(2, 'Ad en az 2 karakter olmalı.').max(120),
  lastName: z.string().min(2, 'Soyad en az 2 karakter olmalı.').max(120),
  email: z.string().email('Geçerli bir e-posta adresi girin.').max(320),
  phoneNumber: z.string().max(40).optional(),
  marketingConsent: z.boolean(),
});

type ProfileFormValues = z.infer<typeof profileSchema>;

export function ProfilePage() {
  const currentCustomerQuery = useCurrentCustomer();
  const updateProfileMutation = useUpdateCustomerProfileMutation();
  const customer = currentCustomerQuery.data;
  const [feedback, setFeedback] = useState<{ open: boolean; severity: 'success' | 'error'; message: string }>({
    open: false,
    severity: 'success',
    message: '',
  });

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors, isDirty },
  } = useForm<ProfileFormValues>({
    resolver: zodResolver(profileSchema),
    defaultValues: {
      firstName: '',
      lastName: '',
      email: '',
      phoneNumber: '',
      marketingConsent: false,
    },
  });

  useEffect(() => {
    if (customer) {
      reset({
        firstName: customer.firstName,
        lastName: customer.lastName,
        email: customer.email,
        phoneNumber: customer.phoneNumber ?? '',
        marketingConsent: customer.marketingConsent,
      });
    }
  }, [customer, reset]);

  const onSubmit = handleSubmit((values) => {
    updateProfileMutation.mutate(
      {
        ...values,
        phoneNumber: values.phoneNumber?.trim() || undefined,
      },
      {
        onSuccess: (updated) => {
          reset({
            firstName: updated.firstName,
            lastName: updated.lastName,
            email: updated.email,
            phoneNumber: updated.phoneNumber ?? '',
            marketingConsent: updated.marketingConsent,
          });
          setFeedback({
            open: true,
            severity: 'success',
            message: 'Profil bilgileriniz güncellendi.',
          });
        },
        onError: (error) => {
          setFeedback({
            open: true,
            severity: 'error',
            message: normalizeApiError(error).message,
          });
        },
      },
    );
  });

  if (currentCustomerQuery.isLoading) {
    return (
      <Stack alignItems="center" sx={{ py: 8 }}>
        <CircularProgress color="secondary" />
      </Stack>
    );
  }

  if (currentCustomerQuery.isError) {
    return <Alert severity="error">Profil bilgileriniz şu anda yüklenemiyor.</Alert>;
  }

  return (
    <Stack spacing={3}>
      <Paper elevation={0} sx={{ border: 1, borderColor: 'divider', p: { xs: 2.5, md: 3 } }}>
        <Stack direction={{ xs: 'column', sm: 'row' }} spacing={1.5} justifyContent="space-between">
          <Box>
            <Typography variant="h4">Profil Bilgileri</Typography>
            <Typography color="text.secondary">Müşteri hesabınız Keycloak oturumu ile eşleştirilir.</Typography>
          </Box>
          <Stack direction="row" spacing={1} flexWrap="wrap">
            <VerifiedChip verified={Boolean(customer?.emailVerified)} label="E-posta" />
            <VerifiedChip verified={Boolean(customer?.phoneVerified)} label="Telefon" />
          </Stack>
        </Stack>
      </Paper>

      <Paper
        component="form"
        elevation={0}
        onSubmit={onSubmit}
        sx={{ border: 1, borderColor: 'divider', p: { xs: 2.5, md: 3 } }}
      >
        <Stack spacing={2.5}>
          <Box
            sx={{
              display: 'grid',
              gridTemplateColumns: { xs: '1fr', md: '1fr 1fr' },
              gap: 2,
            }}
          >
            <TextField
              {...register('firstName')}
              label="Ad"
              error={Boolean(errors.firstName)}
              helperText={errors.firstName?.message ?? ' '}
            />
            <TextField
              {...register('lastName')}
              label="Soyad"
              error={Boolean(errors.lastName)}
              helperText={errors.lastName?.message ?? ' '}
            />
            <TextField
              {...register('email')}
              label="E-posta"
              error={Boolean(errors.email)}
              helperText={errors.email?.message ?? 'Keycloak e-posta claimi ile uyumlu tutulmalı.'}
            />
            <TextField
              {...register('phoneNumber')}
              label="Telefon"
              error={Boolean(errors.phoneNumber)}
              helperText={errors.phoneNumber?.message ?? ' '}
            />
          </Box>

          <FormControlLabel
            control={<Checkbox {...register('marketingConsent')} />}
            label="Kampanya ve koleksiyon bildirimlerini almak istiyorum."
          />

          <Stack direction={{ xs: 'column', sm: 'row' }} spacing={1.5} alignItems={{ sm: 'center' }}>
            <Button
              type="submit"
              variant="contained"
              disabled={!isDirty || updateProfileMutation.isPending}
            >
              {updateProfileMutation.isPending ? 'Kaydediliyor' : 'Profili Kaydet'}
            </Button>
            <Chip label={`Durum: ${customer?.status ?? '-'}`} variant="outlined" />
          </Stack>
        </Stack>
      </Paper>

      <Snackbar
        open={feedback.open}
        autoHideDuration={3000}
        onClose={() => setFeedback((current) => ({ ...current, open: false }))}
      >
        <Alert
          severity={feedback.severity}
          variant="filled"
          onClose={() => setFeedback((current) => ({ ...current, open: false }))}
        >
          {feedback.message}
        </Alert>
      </Snackbar>
    </Stack>
  );
}

function VerifiedChip({ verified, label }: { verified: boolean; label: string }) {
  return (
    <Chip
      icon={verified ? <CheckCircleOutlineIcon /> : <ErrorOutlineIcon />}
      label={`${label} ${verified ? 'doğrulandı' : 'bekliyor'}`}
      color={verified ? 'success' : 'default'}
      variant="outlined"
    />
  );
}
