import { createBrowserRouter } from 'react-router-dom';

import { StorefrontLayout } from '../layouts/StorefrontLayout';
import { CartPage } from '../../modules/storefront/cart/CartPage';
import { HomePage } from '../../modules/storefront/home/HomePage';
import { ProductDetailPage } from '../../modules/storefront/products/ProductDetailPage';
import { ProductsPage } from '../../modules/storefront/products/ProductsPage';
import { SearchPage } from '../../modules/storefront/search/SearchPage';
import { NotFoundPage } from '../../modules/storefront/NotFoundPage';

export const router = createBrowserRouter([
  {
    element: <StorefrontLayout />,
    children: [
      {
        index: true,
        element: <HomePage />,
      },
      {
        path: 'products',
        element: <ProductsPage />,
      },
      {
        path: 'products/:slug',
        element: <ProductDetailPage />,
      },
      {
        path: 'search',
        element: <SearchPage />,
      },
      {
        path: 'cart',
        element: <CartPage />,
      },
      {
        path: '*',
        element: <NotFoundPage />,
      },
    ],
  },
]);

