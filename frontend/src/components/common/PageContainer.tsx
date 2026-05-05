import { Container, ContainerProps } from '@mui/material';

type PageContainerProps = ContainerProps & {
  roomy?: boolean;
};

export function PageContainer({ roomy = true, sx, ...props }: PageContainerProps) {
  return (
    <Container
      {...props}
      sx={{
        py: roomy ? { xs: 4, md: 7 } : undefined,
        ...sx,
      }}
    />
  );
}

