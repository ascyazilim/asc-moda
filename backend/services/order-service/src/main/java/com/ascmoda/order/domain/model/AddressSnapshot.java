package com.ascmoda.order.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class AddressSnapshot {

    @Column(name = "shipping_full_name", nullable = false, length = 160)
    private String fullName;

    @Column(name = "shipping_phone_number", nullable = false, length = 40)
    private String phoneNumber;

    @Column(name = "shipping_city", nullable = false, length = 80)
    private String city;

    @Column(name = "shipping_district", nullable = false, length = 120)
    private String district;

    @Column(name = "shipping_address_line", nullable = false, length = 500)
    private String addressLine;

    @Column(name = "shipping_postal_code", length = 20)
    private String postalCode;

    @Column(name = "shipping_country", nullable = false, length = 80)
    private String country;

    protected AddressSnapshot() {
    }

    public AddressSnapshot(String fullName, String phoneNumber, String city, String district, String addressLine,
                           String postalCode, String country) {
        this.fullName = requireText(fullName, "Shipping full name must be provided");
        this.phoneNumber = requireText(phoneNumber, "Shipping phone number must be provided");
        this.city = requireText(city, "Shipping city must be provided");
        this.district = requireText(district, "Shipping district must be provided");
        this.addressLine = requireText(addressLine, "Shipping address line must be provided");
        this.postalCode = normalizeOptional(postalCode);
        this.country = country == null || country.isBlank() ? "TR" : country.trim();
    }

    private String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }

    private String normalizeOptional(String value) {
        return value == null || value.isBlank() ? null : value.trim();
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
}
