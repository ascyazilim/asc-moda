import { PageContainer } from '../../components/common/PageContainer';
import { EmptyState } from '../../components/common/EmptyState';

export function NotFoundPage() {
  return (
    <PageContainer>
      <EmptyState
        title="Sayfa bulunamadı"
        description="Aradığınız sayfa taşınmış ya da henüz yayına alınmamış olabilir."
        actionLabel="Ana Sayfaya Dön"
        actionHref="/"
      />
    </PageContainer>
  );
}

