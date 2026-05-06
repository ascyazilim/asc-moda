import { CustomerProfile } from '../../types/customer';
import { apiConfig } from './config';

export function resolveCustomerId(
  customer: Pick<CustomerProfile, 'id'> | undefined,
  options: { allowDemoFallback?: boolean } = {},
) {
  if (customer?.id) {
    return customer.id;
  }

  return options.allowDemoFallback ? apiConfig.demoCustomerId : undefined;
}
