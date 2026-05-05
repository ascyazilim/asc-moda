export const productPlaceholderImage =
  'https://images.unsplash.com/photo-1558769132-cb1aea458c5e?auto=format&fit=crop&w=900&q=85';

export const categoryImageBySlug: Record<string, string> = {
  basortu:
    'https://images.unsplash.com/photo-1515886657613-9f3515b0c78f?auto=format&fit=crop&w=900&q=85',
  sal:
    'https://images.unsplash.com/photo-1558769132-cb1aea458c5e?auto=format&fit=crop&w=900&q=85',
  elbise:
    'https://images.unsplash.com/photo-1485968579580-b6d095142e6e?auto=format&fit=crop&w=900&q=85',
  etek:
    'https://images.unsplash.com/photo-1529139574466-a303027c1d8b?auto=format&fit=crop&w=900&q=85',
  bluz:
    'https://images.unsplash.com/photo-1503342217505-b0a15ec3261c?auto=format&fit=crop&w=900&q=85',
  'dis-giyim':
    'https://images.unsplash.com/photo-1542060748-10c28b62716f?auto=format&fit=crop&w=900&q=85',
  tunik:
    'https://images.unsplash.com/photo-1523381210434-271e8be1f52b?auto=format&fit=crop&w=900&q=85',
};

const colorMap: Record<string, string> = {
  bej: '#cbb59c',
  beige: '#cbb59c',
  vizon: '#9a806f',
  taupe: '#a38873',
  moka: '#5a4036',
  kahve: '#5a4036',
  krem: '#eee2d0',
  ivory: '#f4eadb',
  ekru: '#f4eadb',
  siyah: '#1f1b19',
  black: '#1f1b19',
  beyaz: '#f9f6ef',
  white: '#f9f6ef',
  sage: '#8e9a84',
  haki: '#7b8063',
  nude: '#c7a99a',
  pudra: '#c5a39b',
  gül: '#c5a39b',
};

export function getCategoryImage(slug: string) {
  return categoryImageBySlug[slug] ?? productPlaceholderImage;
}

export function getColorValue(name: string) {
  const normalized = name.trim().toLocaleLowerCase('tr-TR');
  const match = Object.entries(colorMap).find(([key]) => normalized.includes(key));

  return match?.[1] ?? '#d8c8b6';
}

