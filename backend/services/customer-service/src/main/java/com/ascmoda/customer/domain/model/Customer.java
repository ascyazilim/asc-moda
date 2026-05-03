package com.ascmoda.customer.domain.model;

import com.ascmoda.customer.domain.exception.IllegalBusinessStateException;
import com.ascmoda.customer.domain.exception.InvalidCustomerStatusTransitionException;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import org.hibernate.annotations.BatchSize;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@BatchSize(size = 50)
@Table(
        name = "customers",
        indexes = {
                @Index(name = "idx_customers_status", columnList = "status"),
                @Index(name = "idx_customers_email", columnList = "email"),
                @Index(name = "idx_customers_created_at", columnList = "created_at")
        }
)
public class Customer extends BaseAuditableEntity {

    @Column(name = "external_user_id", length = 120)
    private String externalUserId;

    @Column(nullable = false, length = 320)
    private String email;

    @Column(name = "phone_number", length = 40)
    private String phoneNumber;

    @Column(name = "first_name", nullable = false, length = 120)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 120)
    private String lastName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private CustomerStatus status = CustomerStatus.ACTIVE;

    @Column(name = "email_verified", nullable = false)
    private boolean emailVerified;

    @Column(name = "phone_verified", nullable = false)
    private boolean phoneVerified;

    @Column(name = "marketing_consent", nullable = false)
    private boolean marketingConsent;

    @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL)
    @BatchSize(size = 50)
    private List<CustomerAddress> addresses = new ArrayList<>();

    protected Customer() {
    }

    public Customer(String externalUserId, String email, String phoneNumber, String firstName, String lastName,
                    boolean emailVerified, boolean phoneVerified, boolean marketingConsent) {
        this.externalUserId = externalUserId;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.firstName = firstName;
        this.lastName = lastName;
        this.status = CustomerStatus.ACTIVE;
        this.emailVerified = emailVerified;
        this.phoneVerified = phoneVerified;
        this.marketingConsent = marketingConsent;
    }

    public void updateProfile(String externalUserId, String email, String phoneNumber, String firstName,
                              String lastName, Boolean emailVerified, Boolean phoneVerified,
                              Boolean marketingConsent) {
        ensureMutable();
        if (externalUserId != null) {
            this.externalUserId = externalUserId;
        }
        if (email != null) {
            this.email = email;
        }
        if (phoneNumber != null) {
            this.phoneNumber = phoneNumber;
        }
        if (firstName != null) {
            this.firstName = firstName;
        }
        if (lastName != null) {
            this.lastName = lastName;
        }
        if (emailVerified != null) {
            this.emailVerified = emailVerified;
        }
        if (phoneVerified != null) {
            this.phoneVerified = phoneVerified;
        }
        if (marketingConsent != null) {
            this.marketingConsent = marketingConsent;
        }
    }

    public void changeStatus(CustomerStatus targetStatus) {
        if (targetStatus == null) {
            throw new InvalidCustomerStatusTransitionException("Customer status must be provided");
        }
        status = targetStatus;
    }

    public void addAddress(CustomerAddress address) {
        address.setCustomer(this);
        addresses.add(address);
    }

    public void ensureMutable() {
        if (status == CustomerStatus.BLOCKED) {
            throw new IllegalBusinessStateException("Blocked customer cannot be modified");
        }
    }

    public String fullName() {
        return firstName + " " + lastName;
    }

    public UUID getId() {
        return super.getId();
    }

    public String getExternalUserId() {
        return externalUserId;
    }

    public String getEmail() {
        return email;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public CustomerStatus getStatus() {
        return status;
    }

    public boolean isEmailVerified() {
        return emailVerified;
    }

    public boolean isPhoneVerified() {
        return phoneVerified;
    }

    public boolean isMarketingConsent() {
        return marketingConsent;
    }

    public List<CustomerAddress> getAddresses() {
        return addresses;
    }
}
