package com.ascmoda.customer.application.service;

import com.ascmoda.customer.application.mapper.CustomerAddressMapper;
import com.ascmoda.customer.controller.dto.CreateCustomerAddressRequest;
import com.ascmoda.customer.controller.dto.CustomerAddressResponse;
import com.ascmoda.customer.controller.dto.CustomerDefaultAddressesResponse;
import com.ascmoda.customer.controller.dto.UpdateCustomerAddressRequest;
import com.ascmoda.customer.domain.exception.CustomerAddressNotFoundException;
import com.ascmoda.customer.domain.exception.CustomerNotFoundException;
import com.ascmoda.customer.domain.exception.InvalidDefaultAddressOperationException;
import com.ascmoda.customer.domain.model.AddressType;
import com.ascmoda.customer.domain.model.Customer;
import com.ascmoda.customer.domain.model.CustomerAddress;
import com.ascmoda.customer.domain.repository.CustomerAddressRepository;
import com.ascmoda.customer.domain.repository.CustomerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class CustomerAddressService {

    private static final Logger log = LoggerFactory.getLogger(CustomerAddressService.class);

    private final CustomerRepository customerRepository;
    private final CustomerAddressRepository customerAddressRepository;
    private final CustomerAddressMapper customerAddressMapper;

    public CustomerAddressService(CustomerRepository customerRepository,
                                  CustomerAddressRepository customerAddressRepository,
                                  CustomerAddressMapper customerAddressMapper) {
        this.customerRepository = customerRepository;
        this.customerAddressRepository = customerAddressRepository;
        this.customerAddressMapper = customerAddressMapper;
    }

    @Transactional
    public CustomerAddressResponse addAddress(UUID customerId, CreateCustomerAddressRequest request) {
        Customer customer = getCustomerEntity(customerId);
        customer.ensureMutable();

        CustomerAddress address = new CustomerAddress(
                normalizeRequired(request.title(), "Address title"),
                request.addressType(),
                normalizeRequired(request.fullName(), "Full name"),
                normalizeRequired(request.phoneNumber(), "Phone number"),
                normalizeRequired(request.city(), "City"),
                normalizeRequired(request.district(), "District"),
                normalizeRequired(request.addressLine(), "Address line"),
                normalizeOptional(request.postalCode()),
                normalizeRequired(request.country(), "Country")
        );
        customer.addAddress(address);

        boolean defaultShipping = request.defaultShipping() || shouldAutoDefaultShipping(customerId, request.addressType());
        boolean defaultBilling = request.defaultBilling() || shouldAutoDefaultBilling(customerId, request.addressType());

        if (defaultShipping) {
            makeDefaultShipping(customerId, address);
        }
        if (defaultBilling) {
            makeDefaultBilling(customerId, address);
        }

        CustomerAddress saved = customerAddressRepository.save(address);
        log.info("Added customer address customerId={} addressId={}", customerId, saved.getId());
        return customerAddressMapper.toResponse(saved);
    }

    @Transactional
    public CustomerAddressResponse updateAddress(UUID customerId, UUID addressId, UpdateCustomerAddressRequest request) {
        CustomerAddress address = getAddressForCustomer(customerId, addressId);
        address.getCustomer().ensureMutable();

        address.update(
                normalizeRequiredIfPresent(request.title(), "Address title"),
                request.addressType(),
                normalizeRequiredIfPresent(request.fullName(), "Full name"),
                normalizeRequiredIfPresent(request.phoneNumber(), "Phone number"),
                normalizeRequiredIfPresent(request.city(), "City"),
                normalizeRequiredIfPresent(request.district(), "District"),
                normalizeRequiredIfPresent(request.addressLine(), "Address line"),
                normalizeOptionalIfPresent(request.postalCode()),
                normalizeRequiredIfPresent(request.country(), "Country")
        );
        ensureCurrentDefaultsAreEligible(address);

        if (request.defaultShipping() != null) {
            if (request.defaultShipping()) {
                makeDefaultShipping(customerId, address);
            } else {
                address.clearDefaultShipping();
            }
        }
        if (request.defaultBilling() != null) {
            if (request.defaultBilling()) {
                makeDefaultBilling(customerId, address);
            } else {
                address.clearDefaultBilling();
            }
        }

        log.info("Updated customer address customerId={} addressId={}", customerId, addressId);
        return customerAddressMapper.toResponse(address);
    }

    @Transactional
    public CustomerAddressResponse deactivateAddress(UUID customerId, UUID addressId) {
        CustomerAddress address = getAddressForCustomer(customerId, addressId);
        address.getCustomer().ensureMutable();
        address.markInactive();
        log.info("Deactivated customer address customerId={} addressId={}", customerId, addressId);
        return customerAddressMapper.toResponse(address);
    }

    @Transactional(readOnly = true)
    public List<CustomerAddressResponse> listAddresses(UUID customerId) {
        ensureCustomerExists(customerId);
        return customerAddressRepository.findByCustomerIdAndActiveTrueOrderByCreatedAtAsc(customerId)
                .stream()
                .map(customerAddressMapper::toResponse)
                .toList();
    }

    @Transactional
    public CustomerAddressResponse setDefaultShippingAddress(UUID customerId, UUID addressId) {
        CustomerAddress address = getAddressForCustomer(customerId, addressId);
        address.getCustomer().ensureMutable();
        makeDefaultShipping(customerId, address);
        log.info("Set default shipping address customerId={} addressId={}", customerId, addressId);
        return customerAddressMapper.toResponse(address);
    }

    @Transactional
    public CustomerAddressResponse setDefaultBillingAddress(UUID customerId, UUID addressId) {
        CustomerAddress address = getAddressForCustomer(customerId, addressId);
        address.getCustomer().ensureMutable();
        makeDefaultBilling(customerId, address);
        log.info("Set default billing address customerId={} addressId={}", customerId, addressId);
        return customerAddressMapper.toResponse(address);
    }

    @Transactional(readOnly = true)
    public CustomerDefaultAddressesResponse getDefaultAddresses(UUID customerId) {
        ensureCustomerExists(customerId);
        return new CustomerDefaultAddressesResponse(
                customerId,
                customerAddressRepository.findByCustomerIdAndDefaultShippingTrueAndActiveTrue(customerId)
                        .map(customerAddressMapper::toResponse)
                        .orElse(null),
                customerAddressRepository.findByCustomerIdAndDefaultBillingTrueAndActiveTrue(customerId)
                        .map(customerAddressMapper::toResponse)
                        .orElse(null)
        );
    }

    private void makeDefaultShipping(UUID customerId, CustomerAddress address) {
        address.ensureActive();
        ensureEligibleForDefaultShipping(address);
        clearDefaultShipping(customerId, address);
        address.makeDefaultShipping();
    }

    private void makeDefaultBilling(UUID customerId, CustomerAddress address) {
        address.ensureActive();
        ensureEligibleForDefaultBilling(address);
        clearDefaultBilling(customerId, address);
        address.makeDefaultBilling();
    }

    private void clearDefaultShipping(UUID customerId, CustomerAddress targetAddress) {
        customerAddressRepository.findByCustomerIdAndDefaultShippingTrueAndActiveTrue(customerId)
                .filter(current -> !current.getId().equals(targetAddress.getId()))
                .ifPresent(current -> {
                    current.clearDefaultShipping();
                    customerAddressRepository.saveAndFlush(current);
                });
    }

    private void clearDefaultBilling(UUID customerId, CustomerAddress targetAddress) {
        customerAddressRepository.findByCustomerIdAndDefaultBillingTrueAndActiveTrue(customerId)
                .filter(current -> !current.getId().equals(targetAddress.getId()))
                .ifPresent(current -> {
                    current.clearDefaultBilling();
                    customerAddressRepository.saveAndFlush(current);
                });
    }

    private boolean shouldAutoDefaultShipping(UUID customerId, AddressType addressType) {
        return addressType != AddressType.BILLING
                && customerAddressRepository.findByCustomerIdAndDefaultShippingTrueAndActiveTrue(customerId).isEmpty();
    }

    private boolean shouldAutoDefaultBilling(UUID customerId, AddressType addressType) {
        return addressType != AddressType.SHIPPING
                && customerAddressRepository.findByCustomerIdAndDefaultBillingTrueAndActiveTrue(customerId).isEmpty();
    }

    private void ensureCurrentDefaultsAreEligible(CustomerAddress address) {
        if (address.isDefaultShipping()) {
            ensureEligibleForDefaultShipping(address);
        }
        if (address.isDefaultBilling()) {
            ensureEligibleForDefaultBilling(address);
        }
    }

    private void ensureEligibleForDefaultShipping(CustomerAddress address) {
        if (address.getAddressType() == AddressType.BILLING) {
            throw new InvalidDefaultAddressOperationException("Billing address cannot be default shipping address");
        }
    }

    private void ensureEligibleForDefaultBilling(CustomerAddress address) {
        if (address.getAddressType() == AddressType.SHIPPING) {
            throw new InvalidDefaultAddressOperationException("Shipping address cannot be default billing address");
        }
    }

    private CustomerAddress getAddressForCustomer(UUID customerId, UUID addressId) {
        return customerAddressRepository.findByIdAndCustomerId(addressId, customerId)
                .orElseThrow(() -> new CustomerAddressNotFoundException("Customer address not found: " + addressId));
    }

    private Customer getCustomerEntity(UUID customerId) {
        return customerRepository.findById(customerId)
                .orElseThrow(() -> new CustomerNotFoundException("Customer not found: " + customerId));
    }

    private void ensureCustomerExists(UUID customerId) {
        if (!customerRepository.existsById(customerId)) {
            throw new CustomerNotFoundException("Customer not found: " + customerId);
        }
    }

    private String normalizeRequiredIfPresent(String value, String fieldName) {
        if (value == null) {
            return null;
        }
        return normalizeRequired(value, fieldName);
    }

    private String normalizeOptionalIfPresent(String value) {
        if (value == null) {
            return null;
        }
        return normalizeOptional(value);
    }

    private String normalizeOptional(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private String normalizeRequired(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must be provided");
        }
        return value.trim();
    }
}
