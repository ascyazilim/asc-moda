package com.ascmoda.customer.domain.repository;

import com.ascmoda.customer.domain.model.Customer;
import com.ascmoda.customer.domain.model.CustomerStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;
import java.util.UUID;

public interface CustomerRepository extends JpaRepository<Customer, UUID>, JpaSpecificationExecutor<Customer> {

    Optional<Customer> findByEmail(String email);

    Optional<Customer> findByExternalUserId(String externalUserId);

    @EntityGraph(attributePaths = "addresses")
    Optional<Customer> findWithAddressesById(UUID id);

    @EntityGraph(attributePaths = "addresses")
    Optional<Customer> findWithAddressesByEmail(String email);

    @EntityGraph(attributePaths = "addresses")
    Optional<Customer> findWithAddressesByExternalUserId(String externalUserId);

    boolean existsByEmail(String email);

    boolean existsByEmailAndIdNot(String email, UUID id);

    boolean existsByExternalUserId(String externalUserId);

    boolean existsByExternalUserIdAndIdNot(String externalUserId, UUID id);

    Page<Customer> findByStatus(CustomerStatus status, Pageable pageable);
}
