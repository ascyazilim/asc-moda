package com.ascmoda.customer.domain.model;

import com.ascmoda.customer.domain.exception.InactiveAddressOperationException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.BatchSize;

@Entity
@BatchSize(size = 50)
@Table(
        name = "customer_addresses",
        indexes = {
                @Index(name = "idx_customer_addresses_customer_id", columnList = "customer_id"),
                @Index(name = "idx_customer_addresses_created_at", columnList = "created_at")
        }
)
public class CustomerAddress extends BaseAuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @Column(nullable = false, length = 80)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(name = "address_type", nullable = false, length = 30)
    private AddressType addressType;

    @Column(name = "full_name", nullable = false, length = 240)
    private String fullName;

    @Column(name = "phone_number", nullable = false, length = 40)
    private String phoneNumber;

    @Column(nullable = false, length = 120)
    private String city;

    @Column(nullable = false, length = 120)
    private String district;

    @Column(name = "address_line", nullable = false, length = 1000)
    private String addressLine;

    @Column(name = "postal_code", length = 20)
    private String postalCode;

    @Column(nullable = false, length = 80)
    private String country;

    @Column(name = "is_default_shipping", nullable = false)
    private boolean defaultShipping;

    @Column(name = "is_default_billing", nullable = false)
    private boolean defaultBilling;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    protected CustomerAddress() {
    }

    public CustomerAddress(String title, AddressType addressType, String fullName, String phoneNumber, String city,
                           String district, String addressLine, String postalCode, String country) {
        this.title = title;
        this.addressType = addressType;
        this.fullName = fullName;
        this.phoneNumber = phoneNumber;
        this.city = city;
        this.district = district;
        this.addressLine = addressLine;
        this.postalCode = postalCode;
        this.country = country;
        this.active = true;
    }

    public void update(String title, AddressType addressType, String fullName, String phoneNumber, String city,
                       String district, String addressLine, String postalCode, String country) {
        ensureActive();
        if (title != null) {
            this.title = title;
        }
        if (addressType != null) {
            this.addressType = addressType;
        }
        if (fullName != null) {
            this.fullName = fullName;
        }
        if (phoneNumber != null) {
            this.phoneNumber = phoneNumber;
        }
        if (city != null) {
            this.city = city;
        }
        if (district != null) {
            this.district = district;
        }
        if (addressLine != null) {
            this.addressLine = addressLine;
        }
        if (postalCode != null) {
            this.postalCode = postalCode;
        }
        if (country != null) {
            this.country = country;
        }
    }

    public void markInactive() {
        active = false;
        defaultShipping = false;
        defaultBilling = false;
    }

    public void makeDefaultShipping() {
        ensureActive();
        defaultShipping = true;
    }

    public void clearDefaultShipping() {
        defaultShipping = false;
    }

    public void makeDefaultBilling() {
        ensureActive();
        defaultBilling = true;
    }

    public void clearDefaultBilling() {
        defaultBilling = false;
    }

    public void ensureActive() {
        if (!active) {
            throw new InactiveAddressOperationException("Inactive address cannot be modified");
        }
    }

    void setCustomer(Customer customer) {
        this.customer = customer;
    }

    public Customer getCustomer() {
        return customer;
    }

    public String getTitle() {
        return title;
    }

    public AddressType getAddressType() {
        return addressType;
    }

    public String getFullName() {
        return fullName;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public String getCity() {
        return city;
    }

    public String getDistrict() {
        return district;
    }

    public String getAddressLine() {
        return addressLine;
    }

    public String getPostalCode() {
        return postalCode;
    }

    public String getCountry() {
        return country;
    }

    public boolean isDefaultShipping() {
        return defaultShipping;
    }

    public boolean isDefaultBilling() {
        return defaultBilling;
    }

    public boolean isActive() {
        return active;
    }
}
