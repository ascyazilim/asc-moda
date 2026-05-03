package com.ascmoda.customer.domain.repository;

import com.ascmoda.customer.domain.model.CustomerAddress;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CustomerAddressRepository extends JpaRepository<CustomerAddress, UUID> {

    List<CustomerAddress> findByCustomerIdOrderByCreatedAtAsc(UUID customerId);

    List<CustomerAddress> findByCustomerIdAndActiveTrueOrderByCreatedAtAsc(UUID customerId);

    Optional<CustomerAddress> findByIdAndCustomerId(UUID id, UUID customerId);

    Optional<CustomerAddress> findByCustomerIdAndDefaultShippingTrueAndActiveTrue(UUID customerId);

    Optional<CustomerAddress> findByCustomerIdAndDefaultBillingTrueAndActiveTrue(UUID customerId);
}
