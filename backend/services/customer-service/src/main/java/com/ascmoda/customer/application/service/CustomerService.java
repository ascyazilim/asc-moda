package com.ascmoda.customer.application.service;

import com.ascmoda.customer.application.mapper.CustomerMapper;
import com.ascmoda.customer.controller.dto.ChangeCustomerStatusRequest;
import com.ascmoda.customer.controller.dto.CreateCustomerRequest;
import com.ascmoda.customer.controller.dto.CustomerDefaultAddressesResponse;
import com.ascmoda.customer.controller.dto.CustomerResponse;
import com.ascmoda.customer.controller.dto.CustomerSummaryResponse;
import com.ascmoda.customer.controller.dto.UpdateCustomerProfileRequest;
import com.ascmoda.customer.domain.exception.CustomerNotFoundException;
import com.ascmoda.customer.domain.exception.DuplicateEmailException;
import com.ascmoda.customer.domain.exception.DuplicateExternalUserIdException;
import com.ascmoda.customer.domain.model.Customer;
import com.ascmoda.customer.domain.model.CustomerAddress;
import com.ascmoda.customer.domain.model.CustomerStatus;
import com.ascmoda.customer.domain.repository.CustomerAddressRepository;
import com.ascmoda.customer.domain.repository.CustomerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.criteria.Predicate;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class CustomerService {

    private static final Logger log = LoggerFactory.getLogger(CustomerService.class);

    private final CustomerRepository customerRepository;
    private final CustomerAddressRepository customerAddressRepository;
    private final CustomerMapper customerMapper;

    public CustomerService(CustomerRepository customerRepository,
                           CustomerAddressRepository customerAddressRepository,
                           CustomerMapper customerMapper) {
        this.customerRepository = customerRepository;
        this.customerAddressRepository = customerAddressRepository;
        this.customerMapper = customerMapper;
    }

    @Transactional
    public CustomerResponse createCustomer(CreateCustomerRequest request) {
        String email = normalizeEmail(request.email());
        String externalUserId = normalizeOptional(request.externalUserId(), "External user id");

        if (externalUserId != null) {
            Customer existing = customerRepository.findWithAddressesByExternalUserId(externalUserId).orElse(null);
            if (existing != null) {
                if (!existing.getEmail().equals(email)) {
                    throw new DuplicateExternalUserIdException("Customer external user id already exists");
                }
                log.info("Customer create matched existing external user customerId={} externalUserId={}",
                        existing.getId(), externalUserId);
                return customerMapper.toResponse(existing);
            }
        }

        ensureEmailAvailable(email);

        Customer customer = new Customer(
                externalUserId,
                email,
                normalizeOptional(request.phoneNumber(), "Phone number"),
                normalizeRequired(request.firstName(), "First name"),
                normalizeRequired(request.lastName(), "Last name"),
                request.emailVerified(),
                request.phoneVerified(),
                request.marketingConsent()
        );

        Customer saved = customerRepository.save(customer);
        log.info("Created customer customerId={} email={}", saved.getId(), saved.getEmail());
        return customerMapper.toResponse(saved);
    }

    @Transactional
    public CustomerResponse updateProfile(UUID customerId, UpdateCustomerProfileRequest request) {
        Customer customer = getCustomerEntity(customerId);

        String email = normalizeEmailIfPresent(request.email());
        String externalUserId = normalizeOptionalIfPresent(request.externalUserId(), "External user id");

        if (email != null) {
            ensureEmailAvailableForUpdate(email, customerId);
        }
        if (externalUserId != null) {
            ensureExternalUserIdAvailableForUpdate(externalUserId, customerId);
        }

        customer.updateProfile(
                externalUserId,
                email,
                normalizeOptionalIfPresent(request.phoneNumber(), "Phone number"),
                normalizeRequiredIfPresent(request.firstName(), "First name"),
                normalizeRequiredIfPresent(request.lastName(), "Last name"),
                request.emailVerified(),
                request.phoneVerified(),
                request.marketingConsent()
        );

        log.info("Updated customer profile customerId={}", customerId);
        return customerMapper.toResponse(customer);
    }

    @Transactional(readOnly = true)
    public CustomerResponse getCustomer(UUID customerId) {
        return customerMapper.toResponse(getCustomerWithAddressesEntity(customerId));
    }

    @Transactional(readOnly = true)
    public CustomerResponse getCustomerByEmail(String email) {
        Customer customer = customerRepository.findWithAddressesByEmail(normalizeEmail(email))
                .orElseThrow(() -> new CustomerNotFoundException("Customer not found by email"));
        return customerMapper.toResponse(customer);
    }

    @Transactional(readOnly = true)
    public CustomerResponse getCustomerByExternalUserId(String externalUserId) {
        Customer customer = customerRepository.findWithAddressesByExternalUserId(
                        normalizeRequired(externalUserId, "External user id")
                )
                .orElseThrow(() -> new CustomerNotFoundException("Customer not found by external user id"));
        return customerMapper.toResponse(customer);
    }

    @Transactional(readOnly = true)
    public CustomerSummaryResponse getSummary(UUID customerId) {
        return toSummary(getCustomerEntity(customerId));
    }

    @Transactional(readOnly = true)
    public CustomerSummaryResponse getSummaryByEmail(String email) {
        Customer customer = customerRepository.findByEmail(normalizeEmail(email))
                .orElseThrow(() -> new CustomerNotFoundException("Customer not found by email"));
        return toSummary(customer);
    }

    @Transactional(readOnly = true)
    public CustomerSummaryResponse getSummaryByExternalUserId(String externalUserId) {
        Customer customer = customerRepository.findByExternalUserId(normalizeRequired(externalUserId, "External user id"))
                .orElseThrow(() -> new CustomerNotFoundException("Customer not found by external user id"));
        return toSummary(customer);
    }

    @Transactional
    public CustomerResponse changeStatus(UUID customerId, ChangeCustomerStatusRequest request) {
        Customer customer = getCustomerWithAddressesEntity(customerId);
        customer.changeStatus(request.status());
        log.info("Changed customer status customerId={} status={}", customerId, request.status());
        return customerMapper.toResponse(customer);
    }

    @Transactional(readOnly = true)
    public Page<CustomerSummaryResponse> listAdmin(CustomerStatus status, String email, String phoneNumber,
                                                   String firstName, String lastName, String externalUserId,
                                                   Instant createdFrom, Instant createdTo, Pageable pageable) {
        return customerRepository.findAll(adminSpecification(
                        status,
                        normalizeEmailFilter(email),
                        normalizeFilter(phoneNumber),
                        normalizeNameFilter(firstName),
                        normalizeNameFilter(lastName),
                        normalizeFilter(externalUserId),
                        createdFrom,
                        createdTo
                ), pageable)
                .map(this::toSummary);
    }

    @Transactional(readOnly = true)
    public CustomerDefaultAddressesResponse getDefaultAddresses(UUID customerId) {
        Customer customer = getCustomerEntity(customerId);
        CustomerAddress shipping = findDefaultShipping(customer.getId());
        CustomerAddress billing = findDefaultBilling(customer.getId());
        CustomerSummaryResponse summary = customerMapper.toSummaryResponse(customer, shipping, billing);
        return new CustomerDefaultAddressesResponse(
                customer.getId(),
                customer.getExternalUserId(),
                customer.fullName(),
                customer.displayName(),
                customer.getStatus(),
                customerAddressRepository.existsByCustomerIdAndActiveTrue(customer.getId()),
                summary.hasDefaultShippingAddress(),
                summary.hasDefaultBillingAddress(),
                summary.defaultShippingAddress(),
                summary.defaultBillingAddress()
        );
    }

    private CustomerSummaryResponse toSummary(Customer customer) {
        return customerMapper.toSummaryResponse(
                customer,
                findDefaultShipping(customer.getId()),
                findDefaultBilling(customer.getId())
        );
    }

    private CustomerAddress findDefaultShipping(UUID customerId) {
        return customerAddressRepository.findByCustomerIdAndDefaultShippingTrueAndActiveTrue(customerId)
                .orElse(null);
    }

    private CustomerAddress findDefaultBilling(UUID customerId) {
        return customerAddressRepository.findByCustomerIdAndDefaultBillingTrueAndActiveTrue(customerId)
                .orElse(null);
    }

    private Customer getCustomerEntity(UUID customerId) {
        return customerRepository.findById(customerId)
                .orElseThrow(() -> new CustomerNotFoundException("Customer not found: " + customerId));
    }

    private Customer getCustomerWithAddressesEntity(UUID customerId) {
        return customerRepository.findWithAddressesById(customerId)
                .orElseThrow(() -> new CustomerNotFoundException("Customer not found: " + customerId));
    }

    private void ensureEmailAvailable(String email) {
        if (customerRepository.existsByEmail(email)) {
            throw new DuplicateEmailException("Customer email already exists");
        }
    }

    private void ensureEmailAvailableForUpdate(String email, UUID customerId) {
        if (customerRepository.existsByEmailAndIdNot(email, customerId)) {
            throw new DuplicateEmailException("Customer email already exists");
        }
    }

    private void ensureExternalUserIdAvailableForUpdate(String externalUserId, UUID customerId) {
        if (customerRepository.existsByExternalUserIdAndIdNot(externalUserId, customerId)) {
            throw new DuplicateExternalUserIdException("Customer external user id already exists");
        }
    }

    private String normalizeEmail(String email) {
        return normalizeRequired(email, "Email").toLowerCase(Locale.ROOT);
    }

    private String normalizeEmailIfPresent(String email) {
        if (email == null) {
            return null;
        }
        return normalizeEmail(email);
    }

    private String normalizeRequiredIfPresent(String value, String fieldName) {
        if (value == null) {
            return null;
        }
        return normalizeRequired(value, fieldName);
    }

    private String normalizeOptionalIfPresent(String value, String fieldName) {
        if (value == null) {
            return null;
        }
        return normalizeRequired(value, fieldName);
    }

    private String normalizeOptional(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return normalizeRequired(value, fieldName);
    }

    private Specification<Customer> adminSpecification(CustomerStatus status, String email, String phoneNumber,
                                                       String firstName, String lastName, String externalUserId,
                                                       Instant createdFrom, Instant createdTo) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (status != null) {
                predicates.add(criteriaBuilder.equal(root.get("status"), status));
            }
            if (email != null) {
                predicates.add(criteriaBuilder.like(root.get("email"), "%" + email + "%"));
            }
            if (phoneNumber != null) {
                predicates.add(criteriaBuilder.like(root.get("phoneNumber"), "%" + phoneNumber + "%"));
            }
            if (firstName != null) {
                predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.get("firstName")),
                        "%" + firstName + "%"));
            }
            if (lastName != null) {
                predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.get("lastName")),
                        "%" + lastName + "%"));
            }
            if (externalUserId != null) {
                predicates.add(criteriaBuilder.like(root.get("externalUserId"), "%" + externalUserId + "%"));
            }
            if (createdFrom != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("createdAt"), createdFrom));
            }
            if (createdTo != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("createdAt"), createdTo));
            }
            return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
        };
    }

    private String normalizeEmailFilter(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeFilter(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private String normalizeNameFilter(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeRequired(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must be provided");
        }
        return value.trim();
    }
}
