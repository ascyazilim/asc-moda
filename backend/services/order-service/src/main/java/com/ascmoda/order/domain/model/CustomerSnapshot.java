package com.ascmoda.order.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class CustomerSnapshot {

    @Column(name = "customer_full_name_snapshot", nullable = false, length = 160)
    private String fullName;

    @Column(name = "customer_phone_number_snapshot", nullable = false, length = 40)
    private String phoneNumber;

    protected CustomerSnapshot() {
    }

    public CustomerSnapshot(String fullName, String phoneNumber) {
        this.fullName = requireText(fullName, "Customer full name snapshot must be provided");
        this.phoneNumber = requireText(phoneNumber, "Customer phone number snapshot must be provided");
    }

    private String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }

    public String getFullName() {
        return fullName;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }
}
