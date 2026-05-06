import AddLocationAltOutlinedIcon from '@mui/icons-material/AddLocationAltOutlined';
import DeleteOutlineIcon from '@mui/icons-material/DeleteOutline';
import EditOutlinedIcon from '@mui/icons-material/EditOutlined';
import HomeOutlinedIcon from '@mui/icons-material/HomeOutlined';
import LocalShippingOutlinedIcon from '@mui/icons-material/LocalShippingOutlined';
import ReceiptLongOutlinedIcon from '@mui/icons-material/ReceiptLongOutlined';
import {
  Alert,
  Box,
  Button,
  Checkbox,
  Chip,
  CircularProgress,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  FormControlLabel,
  IconButton,
  MenuItem,
  Paper,
  Snackbar,
  Stack,
  TextField,
  Typography,
} from '@mui/material';
import { zodResolver } from '@hookform/resolvers/zod';
import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { z } from 'zod';

import { EmptyState } from '../../../components/common/EmptyState';
import {
  useAddAddressMutation,
  useCustomerAddresses,
  useCurrentCustomer,
  useDeactivateAddressMutation,
  useSetDefaultBillingAddressMutation,
  useSetDefaultShippingAddressMutation,
  useUpdateAddressMutation,
} from '../../../hooks/useAccountQueries';
import { normalizeApiError } from '../../../services/api/client';
import { AddressType, CustomerAddress } from '../../../types/customer';

const addressSchema = z.object({
  title: z.string().min(2, 'Başlık en az 2 karakter olmalı.').max(80),
  addressType: z.enum(['SHIPPING', 'BILLING', 'OTHER']),
  fullName: z.string().min(3, 'Ad soyad en az 3 karakter olmalı.').max(240),
  phoneNumber: z.string().min(7, 'Telefon numarası kısa görünüyor.').max(40),
  city: z.string().min(2, 'Şehir zorunlu.').max(120),
  district: z.string().min(2, 'İlçe zorunlu.').max(120),
  addressLine: z.string().min(8, 'Adres satırı daha detaylı olmalı.').max(1000),
  postalCode: z.string().max(20).optional(),
  country: z.string().min(2, 'Ülke zorunlu.').max(80),
  defaultShipping: z.boolean(),
  defaultBilling: z.boolean(),
});

type AddressFormValues = z.infer<typeof addressSchema>;

const emptyAddressValues: AddressFormValues = {
  title: '',
  addressType: 'SHIPPING',
  fullName: '',
  phoneNumber: '',
  city: '',
  district: '',
  addressLine: '',
  postalCode: '',
  country: 'Türkiye',
  defaultShipping: false,
  defaultBilling: false,
};

export function AddressesPage() {
  const currentCustomerQuery = useCurrentCustomer();
  const addressesQuery = useCustomerAddresses();
  const addAddressMutation = useAddAddressMutation();
  const updateAddressMutation = useUpdateAddressMutation();
  const deactivateAddressMutation = useDeactivateAddressMutation();
  const setDefaultShippingMutation = useSetDefaultShippingAddressMutation();
  const setDefaultBillingMutation = useSetDefaultBillingAddressMutation();
  const [dialogOpen, setDialogOpen] = useState(false);
  const [editingAddress, setEditingAddress] = useState<CustomerAddress | null>(null);
  const [feedback, setFeedback] = useState<{ open: boolean; severity: 'success' | 'error'; message: string }>({
    open: false,
    severity: 'success',
    message: '',
  });

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<AddressFormValues>({
    resolver: zodResolver(addressSchema),
    defaultValues: emptyAddressValues,
  });

  const addresses = (addressesQuery.data ?? []).filter((address) => address.active);
  const isLoading = currentCustomerQuery.isLoading || addressesQuery.isLoading;
  const isMutating =
    addAddressMutation.isPending ||
    updateAddressMutation.isPending ||
    deactivateAddressMutation.isPending ||
    setDefaultShippingMutation.isPending ||
    setDefaultBillingMutation.isPending;

  const openCreateDialog = () => {
    setEditingAddress(null);
    reset(emptyAddressValues);
    setDialogOpen(true);
  };

  const openEditDialog = (address: CustomerAddress) => {
    setEditingAddress(address);
    reset(toFormValues(address));
    setDialogOpen(true);
  };

  const closeDialog = () => {
    setDialogOpen(false);
    setEditingAddress(null);
  };

  const onSubmit = handleSubmit((values) => {
    const payload = {
      ...values,
      postalCode: values.postalCode?.trim() || undefined,
    };

    const options = {
      onSuccess: () => {
        setFeedback({
          open: true,
          severity: 'success' as const,
          message: editingAddress ? 'Adres güncellendi.' : 'Adres eklendi.',
        });
        closeDialog();
      },
      onError: (error: unknown) => {
        setFeedback({
          open: true,
          severity: 'error' as const,
          message: normalizeApiError(error).message,
        });
      },
    };

    if (editingAddress) {
      updateAddressMutation.mutate({ addressId: editingAddress.id, payload }, options);
      return;
    }

    addAddressMutation.mutate(payload, options);
  });

  const deactivateAddress = (address: CustomerAddress) => {
    if (!window.confirm(`${address.title} adresi pasifleştirilsin mi?`)) {
      return;
    }

    deactivateAddressMutation.mutate(address.id, {
      onSuccess: () =>
        setFeedback({
          open: true,
          severity: 'success',
          message: 'Adres pasifleştirildi.',
        }),
      onError: (error) =>
        setFeedback({
          open: true,
          severity: 'error',
          message: normalizeApiError(error).message,
        }),
    });
  };

  return (
    <Stack spacing={3}>
      <Paper elevation={0} sx={{ border: 1, borderColor: 'divider', p: { xs: 2.5, md: 3 } }}>
        <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2} justifyContent="space-between">
          <Box>
            <Typography variant="h4">Adreslerim</Typography>
            <Typography color="text.secondary">
              Teslimat ve fatura adreslerinizi buradan yönetin.
            </Typography>
          </Box>
          <Button
            variant="contained"
            startIcon={<AddLocationAltOutlinedIcon />}
            onClick={openCreateDialog}
            disabled={isLoading}
          >
            Yeni Adres
          </Button>
        </Stack>
      </Paper>

      {isLoading ? (
        <Stack alignItems="center" sx={{ py: 8 }}>
          <CircularProgress color="secondary" />
        </Stack>
      ) : null}

      {addressesQuery.isError || currentCustomerQuery.isError ? (
        <Alert severity="error">Adres bilgileriniz şu anda yüklenemiyor.</Alert>
      ) : null}

      {!isLoading && !addresses.length && !addressesQuery.isError ? (
        <EmptyState
          title="Kayıtlı adres yok"
          description="Sipariş sürecine hazır olmak için teslimat adresinizi ekleyebilirsiniz."
        />
      ) : null}

      {!isLoading && addresses.length ? (
        <Box
          sx={{
            display: 'grid',
            gridTemplateColumns: { xs: '1fr', lg: 'repeat(2, minmax(0, 1fr))' },
            gap: 2,
          }}
        >
          {addresses.map((address) => (
            <AddressCard
              key={address.id}
              address={address}
              disabled={isMutating}
              onEdit={() => openEditDialog(address)}
              onDelete={() => deactivateAddress(address)}
              onSetShipping={() =>
                setDefaultShippingMutation.mutate(address.id, {
                  onSuccess: () =>
                    setFeedback({
                      open: true,
                      severity: 'success',
                      message: 'Varsayılan teslimat adresi güncellendi.',
                    }),
                })
              }
              onSetBilling={() =>
                setDefaultBillingMutation.mutate(address.id, {
                  onSuccess: () =>
                    setFeedback({
                      open: true,
                      severity: 'success',
                      message: 'Varsayılan fatura adresi güncellendi.',
                    }),
                })
              }
            />
          ))}
        </Box>
      ) : null}

      <Dialog open={dialogOpen} onClose={closeDialog} fullWidth maxWidth="md">
        <DialogTitle>{editingAddress ? 'Adresi Düzenle' : 'Yeni Adres'}</DialogTitle>
        <DialogContent>
          <Box component="form" id="address-form" onSubmit={onSubmit} sx={{ pt: 1 }}>
            <Box
              sx={{
                display: 'grid',
                gridTemplateColumns: { xs: '1fr', md: '1fr 1fr' },
                gap: 2,
              }}
            >
              <TextField
                {...register('title')}
                label="Adres başlığı"
                error={Boolean(errors.title)}
                helperText={errors.title?.message ?? ' '}
              />
              <TextField
                {...register('addressType')}
                select
                label="Adres tipi"
                error={Boolean(errors.addressType)}
                helperText={errors.addressType?.message ?? ' '}
              >
                <MenuItem value="SHIPPING">Teslimat</MenuItem>
                <MenuItem value="BILLING">Fatura</MenuItem>
                <MenuItem value="OTHER">Diğer</MenuItem>
              </TextField>
              <TextField
                {...register('fullName')}
                label="Ad soyad"
                error={Boolean(errors.fullName)}
                helperText={errors.fullName?.message ?? ' '}
              />
              <TextField
                {...register('phoneNumber')}
                label="Telefon"
                error={Boolean(errors.phoneNumber)}
                helperText={errors.phoneNumber?.message ?? ' '}
              />
              <TextField
                {...register('city')}
                label="Şehir"
                error={Boolean(errors.city)}
                helperText={errors.city?.message ?? ' '}
              />
              <TextField
                {...register('district')}
                label="İlçe"
                error={Boolean(errors.district)}
                helperText={errors.district?.message ?? ' '}
              />
              <TextField
                {...register('postalCode')}
                label="Posta kodu"
                error={Boolean(errors.postalCode)}
                helperText={errors.postalCode?.message ?? ' '}
              />
              <TextField
                {...register('country')}
                label="Ülke"
                error={Boolean(errors.country)}
                helperText={errors.country?.message ?? ' '}
              />
            </Box>
            <TextField
              {...register('addressLine')}
              label="Adres"
              multiline
              minRows={3}
              fullWidth
              error={Boolean(errors.addressLine)}
              helperText={errors.addressLine?.message ?? ' '}
              sx={{ mt: 2 }}
            />
            <Stack direction={{ xs: 'column', sm: 'row' }} spacing={1}>
              <FormControlLabel
                control={<Checkbox {...register('defaultShipping')} />}
                label="Varsayılan teslimat adresi"
              />
              <FormControlLabel
                control={<Checkbox {...register('defaultBilling')} />}
                label="Varsayılan fatura adresi"
              />
            </Stack>
          </Box>
        </DialogContent>
        <DialogActions sx={{ px: 3, pb: 3 }}>
          <Button color="inherit" onClick={closeDialog}>
            Vazgeç
          </Button>
          <Button type="submit" form="address-form" variant="contained" disabled={isMutating}>
            {isMutating ? 'Kaydediliyor' : 'Kaydet'}
          </Button>
        </DialogActions>
      </Dialog>

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

type AddressCardProps = {
  address: CustomerAddress;
  disabled: boolean;
  onEdit: () => void;
  onDelete: () => void;
  onSetShipping: () => void;
  onSetBilling: () => void;
};

function AddressCard({
  address,
  disabled,
  onEdit,
  onDelete,
  onSetShipping,
  onSetBilling,
}: AddressCardProps) {
  return (
    <Paper elevation={0} sx={{ border: 1, borderColor: 'divider', p: 2.5 }}>
      <Stack spacing={2}>
        <Stack direction="row" justifyContent="space-between" spacing={1.5}>
          <Stack direction="row" spacing={1.25} alignItems="center">
            <HomeOutlinedIcon color="secondary" />
            <Box>
              <Typography variant="h5">{address.title}</Typography>
              <Typography variant="body2" color="text.secondary">
                {addressTypeLabel(address.addressType)}
              </Typography>
            </Box>
          </Stack>
          <Stack direction="row" spacing={0.5}>
            <IconButton aria-label="Adresi düzenle" onClick={onEdit} disabled={disabled}>
              <EditOutlinedIcon />
            </IconButton>
            <IconButton aria-label="Adresi sil" onClick={onDelete} disabled={disabled}>
              <DeleteOutlineIcon />
            </IconButton>
          </Stack>
        </Stack>

        <Stack spacing={0.5}>
          <Typography fontWeight={700}>{address.fullName}</Typography>
          <Typography color="text.secondary">{address.phoneNumber}</Typography>
          <Typography color="text.secondary">
            {address.addressLine}, {address.district} / {address.city}
          </Typography>
          <Typography color="text.secondary">
            {[address.postalCode, address.country].filter(Boolean).join(' - ')}
          </Typography>
        </Stack>

        <Stack direction="row" spacing={1} flexWrap="wrap">
          {address.defaultShipping ? (
            <Chip icon={<LocalShippingOutlinedIcon />} label="Varsayılan teslimat" color="success" variant="outlined" />
          ) : null}
          {address.defaultBilling ? (
            <Chip icon={<ReceiptLongOutlinedIcon />} label="Varsayılan fatura" color="success" variant="outlined" />
          ) : null}
        </Stack>

        <Stack direction={{ xs: 'column', sm: 'row' }} spacing={1}>
          <Button
            variant="outlined"
            onClick={onSetShipping}
            disabled={disabled || address.defaultShipping}
          >
            Teslimat Yap
          </Button>
          <Button
            variant="outlined"
            onClick={onSetBilling}
            disabled={disabled || address.defaultBilling}
          >
            Fatura Yap
          </Button>
        </Stack>
      </Stack>
    </Paper>
  );
}

function toFormValues(address: CustomerAddress): AddressFormValues {
  return {
    title: address.title,
    addressType: address.addressType,
    fullName: address.fullName,
    phoneNumber: address.phoneNumber,
    city: address.city,
    district: address.district,
    addressLine: address.addressLine,
    postalCode: address.postalCode ?? '',
    country: address.country,
    defaultShipping: address.defaultShipping,
    defaultBilling: address.defaultBilling,
  };
}

function addressTypeLabel(type: AddressType) {
  if (type === 'BILLING') {
    return 'Fatura';
  }

  if (type === 'OTHER') {
    return 'Diğer';
  }

  return 'Teslimat';
}
