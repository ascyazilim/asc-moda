import SearchIcon from '@mui/icons-material/Search';
import { IconButton, InputAdornment, TextField, TextFieldProps } from '@mui/material';

type SearchInputProps = Omit<TextFieldProps, 'onSubmit'> & {
  onSearch?: () => void;
};

export function SearchInput({ onSearch, InputProps, ...props }: SearchInputProps) {
  return (
    <TextField
      {...props}
      fullWidth
      placeholder={props.placeholder ?? 'Ürün, kategori veya renk ara'}
      InputProps={{
        ...InputProps,
        startAdornment: (
          <InputAdornment position="start">
            <SearchIcon fontSize="small" />
          </InputAdornment>
        ),
        endAdornment: onSearch ? (
          <InputAdornment position="end">
            <IconButton aria-label="Ara" onClick={onSearch} edge="end">
              <SearchIcon fontSize="small" />
            </IconButton>
          </InputAdornment>
        ) : (
          InputProps?.endAdornment
        ),
      }}
    />
  );
}

