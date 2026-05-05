import { CartItem } from '../../../types/cart';
import {
  Category,
  Product,
  ProductCategory,
  ProductFilters,
  ProductSort,
} from '../../../types/product';

export const heroImage =
  'https://images.unsplash.com/photo-1496747611176-843222e1e57c?auto=format&fit=crop&w=1400&q=85';

const imageSet = {
  neutralDress:
    'https://images.unsplash.com/photo-1485968579580-b6d095142e6e?auto=format&fit=crop&w=900&q=85',
  silkScarf:
    'https://images.unsplash.com/photo-1515886657613-9f3515b0c78f?auto=format&fit=crop&w=900&q=85',
  softBlouse:
    'https://images.unsplash.com/photo-1503342217505-b0a15ec3261c?auto=format&fit=crop&w=900&q=85',
  outerwear:
    'https://images.unsplash.com/photo-1542060748-10c28b62716f?auto=format&fit=crop&w=900&q=85',
  skirt:
    'https://images.unsplash.com/photo-1529139574466-a303027c1d8b?auto=format&fit=crop&w=900&q=85',
  textile:
    'https://images.unsplash.com/photo-1558769132-cb1aea458c5e?auto=format&fit=crop&w=900&q=85',
  rack:
    'https://images.unsplash.com/photo-1445205170230-053b83016050?auto=format&fit=crop&w=900&q=85',
  linen:
    'https://images.unsplash.com/photo-1523381210434-271e8be1f52b?auto=format&fit=crop&w=900&q=85',
};

export const storefrontCategories: Category[] = [
  {
    id: 'basortu',
    title: 'Başörtü',
    description: 'Zarif dokular ve günlük şıklığa uyumlu tonlar.',
    image: imageSet.silkScarf,
    href: '/products?category=basortu',
  },
  {
    id: 'sal',
    title: 'Şal',
    description: 'Yumuşak düşümlü, sezonun sakin renkleri.',
    image: imageSet.textile,
    href: '/products?category=sal',
  },
  {
    id: 'elbise',
    title: 'Elbise',
    description: 'Özel günlerden günlük stile uzanan premium formlar.',
    image: imageSet.neutralDress,
    href: '/products?category=elbise',
  },
  {
    id: 'etek',
    title: 'Etek',
    description: 'Akışkan kesimler ve rahat siluetler.',
    image: imageSet.skirt,
    href: '/products?category=etek',
  },
  {
    id: 'bluz',
    title: 'Bluz',
    description: 'Katmanlı kombinlere sade ve güçlü tamamlayıcılar.',
    image: imageSet.softBlouse,
    href: '/products?category=bluz',
  },
  {
    id: 'dis-giyim',
    title: 'Dış Giyim',
    description: 'Mevsim geçişlerine uyumlu, zamansız parçalar.',
    image: imageSet.outerwear,
    href: '/products?category=dis-giyim',
  },
];

export const categoryLabels: Record<ProductCategory, string> = {
  basortu: 'Başörtü',
  sal: 'Şal',
  elbise: 'Elbise',
  etek: 'Etek',
  bluz: 'Bluz',
  'dis-giyim': 'Dış Giyim',
  tunik: 'Tunik',
};

export const storefrontProducts: Product[] = [
  {
    id: 'prd-001',
    slug: 'ipek-dokulu-vizon-sal',
    name: 'İpek Dokulu Vizon Şal',
    category: 'sal',
    categoryLabel: categoryLabels.sal,
    price: 890,
    compareAtPrice: 1090,
    description:
      'Hafif parlak dokusu ve yumuşak tutuşuyla hem gündüz hem davet kombinlerine eşlik eder.',
    details: [
      'İpeksi dokulu premium kumaş',
      'Kayma yapmayan dengeli yüzey',
      'Sezonun sıcak vizon tonu',
    ],
    images: [imageSet.silkScarf, imageSet.textile, imageSet.linen],
    colors: [
      { name: 'Vizon', value: '#9a806f' },
      { name: 'Ivory', value: '#f3eadc' },
      { name: 'Moka', value: '#5a4036' },
    ],
    sizes: ['Standart'],
    isFeatured: true,
    isNew: true,
    stock: 18,
  },
  {
    id: 'prd-002',
    slug: 'minimal-kemerli-krem-elbise',
    name: 'Minimal Kemerli Krem Elbise',
    category: 'elbise',
    categoryLabel: categoryLabels.elbise,
    price: 2490,
    description:
      'Bel hattını nazikçe toparlayan kemerli formu ve akışkan kumaşıyla sade bir duruş sunar.',
    details: [
      'Astarlı, tok duruşlu kumaş',
      'Çıkarılabilir kumaş kemer',
      'Bileğe yakın modern boy',
    ],
    images: [imageSet.neutralDress, imageSet.rack, imageSet.textile],
    colors: [
      { name: 'Krem', value: '#eee2d0' },
      { name: 'Taupe', value: '#a38873' },
    ],
    sizes: ['36', '38', '40', '42', '44'],
    isFeatured: true,
    stock: 9,
  },
  {
    id: 'prd-003',
    slug: 'akiskan-kup-bej-etek',
    name: 'Akışkan Kup Bej Etek',
    category: 'etek',
    categoryLabel: categoryLabels.etek,
    price: 1490,
    description:
      'Günlük konforu premium bir çizgiyle buluşturan, hareketli ve zarif etek formu.',
    details: [
      'Rahat bel yapısı',
      'Dökümlü ve kırışmaya dayanıklı yüzey',
      'Bot ve loafer kombinlerine uyumlu boy',
    ],
    images: [imageSet.skirt, imageSet.linen, imageSet.rack],
    colors: [
      { name: 'Bej', value: '#cbb59c' },
      { name: 'Moka', value: '#5a4036' },
    ],
    sizes: ['36', '38', '40', '42'],
    isFeatured: true,
    isNew: true,
    stock: 14,
  },
  {
    id: 'prd-004',
    slug: 'soft-dokulu-saten-basortu',
    name: 'Soft Dokulu Saten Başörtü',
    category: 'basortu',
    categoryLabel: categoryLabels.basortu,
    price: 690,
    description:
      'Satenin zarif ışığını günlük kullanıma uygun yumuşak bir dokuyla yorumlar.',
    details: [
      'Dengeli parlaklık',
      'Tok ve yumuşak tuşe',
      'Özel gün stiline uygun bitiş',
    ],
    images: [imageSet.textile, imageSet.silkScarf, imageSet.linen],
    colors: [
      { name: 'Ivory', value: '#f4eadb' },
      { name: 'Gül Kurusu', value: '#c5a39b' },
      { name: 'Sage', value: '#8e9a84' },
    ],
    sizes: ['Standart'],
    isFeatured: true,
    stock: 23,
  },
  {
    id: 'prd-005',
    slug: 'katmanli-yaka-moka-bluz',
    name: 'Katmanlı Yaka Moka Bluz',
    category: 'bluz',
    categoryLabel: categoryLabels.bluz,
    price: 1190,
    description:
      'İç göstermez dokusu, temiz yaka formu ve sakin rengiyle ofis ve günlük stile uyarlanır.',
    details: [
      'Nefes alan mat kumaş',
      'İç göstermeyen tok yapı',
      'Ceket ve triko altında rahat kullanım',
    ],
    images: [imageSet.softBlouse, imageSet.rack, imageSet.neutralDress],
    colors: [
      { name: 'Moka', value: '#5a4036' },
      { name: 'Krem', value: '#eee2d0' },
    ],
    sizes: ['36', '38', '40', '42', '44'],
    isNew: true,
    stock: 11,
  },
  {
    id: 'prd-006',
    slug: 'hafif-dokulu-trenc-kap',
    name: 'Hafif Dokulu Trenç Kap',
    category: 'dis-giyim',
    categoryLabel: categoryLabels['dis-giyim'],
    price: 3290,
    compareAtPrice: 3690,
    description:
      'Mevsim geçişleri için tasarlanan, omuz hattı yumuşak ve tok görünümlü dış giyim parçası.',
    details: [
      'Su itici hafif yüzey',
      'Kuşaklı ve rahat kalıp',
      'İç katmanlarla uyumlu genişlik',
    ],
    images: [imageSet.outerwear, imageSet.rack, imageSet.neutralDress],
    colors: [
      { name: 'Taş', value: '#d8c8b6' },
      { name: 'Koyu Vizon', value: '#7b6455' },
    ],
    sizes: ['S', 'M', 'L', 'XL'],
    isFeatured: true,
    stock: 7,
  },
  {
    id: 'prd-007',
    slug: 'rahat-kesim-ivory-tunik',
    name: 'Rahat Kesim Ivory Tunik',
    category: 'tunik',
    categoryLabel: categoryLabels.tunik,
    price: 1590,
    description:
      'Uzun kesimi ve sakin formuyla katmanlı tesettür kombinlerinin ana parçası olur.',
    details: [
      'Yan yırtmaçlı rahat form',
      'Dökümlü kumaş',
      'Pantolon ve etekle uyumlu uzunluk',
    ],
    images: [imageSet.linen, imageSet.softBlouse, imageSet.rack],
    colors: [
      { name: 'Ivory', value: '#f4eadb' },
      { name: 'Sage', value: '#8e9a84' },
    ],
    sizes: ['S', 'M', 'L', 'XL'],
    isNew: true,
    stock: 16,
  },
  {
    id: 'prd-008',
    slug: 'premium-nude-sal',
    name: 'Premium Nude Şal',
    category: 'sal',
    categoryLabel: categoryLabels.sal,
    price: 790,
    description:
      'Mat bitişli nude tonu ve hafif yapısıyla minimal kombinlerin vazgeçilmez tamamlayıcısı.',
    details: [
      'Mat ve yumuşak bitiş',
      'Dört mevsim kullanılabilir gramaj',
      'Kolay şekil alan dokuma',
    ],
    images: [imageSet.textile, imageSet.silkScarf, imageSet.linen],
    colors: [
      { name: 'Nude', value: '#c7a99a' },
      { name: 'Kum', value: '#d8c2a7' },
    ],
    sizes: ['Standart'],
    stock: 28,
  },
];

export const mockCartItems: CartItem[] = [
  {
    id: 'cart-001',
    product: storefrontProducts[0],
    color: 'Vizon',
    size: 'Standart',
    quantity: 1,
  },
  {
    id: 'cart-002',
    product: storefrontProducts[2],
    color: 'Bej',
    size: '38',
    quantity: 1,
  },
];

export function getProductBySlug(slug: string) {
  return storefrontProducts.find((product) => product.slug === slug);
}

export function getProductsByCategory(category: ProductCategory) {
  return storefrontProducts.filter((product) => product.category === category);
}

export function filterProducts(filters: ProductFilters = {}) {
  const priceRange = filters.priceRange ?? [0, 4000];
  const query = filters.query?.trim().toLocaleLowerCase('tr-TR');

  const filtered = storefrontProducts.filter((product) => {
    const matchesCategory =
      !filters.category || filters.category === 'all'
        ? true
        : product.category === filters.category;
    const matchesPrice = product.price >= priceRange[0] && product.price <= priceRange[1];
    const matchesQuery = query
      ? `${product.name} ${product.categoryLabel} ${product.description}`
          .toLocaleLowerCase('tr-TR')
          .includes(query)
      : true;
    const matchesColors =
      !filters.colors?.length ||
      product.colors.some((color) => filters.colors?.includes(color.name));
    const matchesSizes =
      !filters.sizes?.length || product.sizes.some((size) => filters.sizes?.includes(size));

    return matchesCategory && matchesPrice && matchesQuery && matchesColors && matchesSizes;
  });

  return sortProducts(filtered, filters.sort);
}

function sortProducts(products: Product[], sort: ProductSort = 'featured') {
  return [...products].sort((a, b) => {
    if (sort === 'newest') {
      return Number(Boolean(b.isNew)) - Number(Boolean(a.isNew));
    }

    if (sort === 'priceAsc') {
      return a.price - b.price;
    }

    if (sort === 'priceDesc') {
      return b.price - a.price;
    }

    return Number(Boolean(b.isFeatured)) - Number(Boolean(a.isFeatured));
  });
}

