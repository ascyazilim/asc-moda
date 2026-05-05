export function formatCurrency(value: number) {
  return new Intl.NumberFormat('tr-TR', {
    style: 'currency',
    currency: 'TRY',
    maximumFractionDigits: 0,
  }).format(value);
}

export function clampQuantity(value: number, min = 1, max = 10) {
  return Math.min(Math.max(value, min), max);
}

