import { createBrowserRouter } from 'react-router-dom';

import { ProtectedRoute } from '../auth/ProtectedRoute';
import { StorefrontLayout } from '../layouts/StorefrontLayout';
import { AccountLayout } from '../../modules/storefront/account/AccountLayout';
import { AccountOverviewPage } from '../../modules/storefront/account/AccountOverviewPage';
import { AddressesPage } from '../../modules/storefront/account/AddressesPage';
import { OrderDetailPage } from '../../modules/storefront/account/OrderDetailPage';
import { OrdersPage } from '../../modules/storefront/account/OrdersPage';
import { ProfilePage } from '../../modules/storefront/account/ProfilePage';
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
        element: <ProtectedRoute />,
        children: [
          {
            path: 'cart',
            element: <CartPage />,
          },
          {
            path: 'account',
            element: <AccountLayout />,
            children: [
              {
                index: true,
                element: <AccountOverviewPage />,
              },
              {
                path: 'profile',
                element: <ProfilePage />,
              },
              {
                path: 'addresses',
                element: <AddressesPage />,
              },
              {
                path: 'orders',
                element: <OrdersPage />,
              },
              {
                path: 'orders/:orderId',
                element: <OrderDetailPage />,
              },
            ],
          },
        ],
      },
      {
        path: '*',
        element: <NotFoundPage />,
      },
    ],
  },
]);
