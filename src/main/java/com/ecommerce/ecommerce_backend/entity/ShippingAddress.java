package com.ecommerce.ecommerce_backend.entity;

import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;

@Embeddable
public class ShippingAddress {
    @NotEmpty(message = "Full name must not be empty")
    private String fullName;

    @NotEmpty(message = "Street address must not be empty")
    private String streetAddress;

    @NotEmpty(message = "City must not be empty")
    private String city;

    @NotEmpty(message = "Postal code must not be empty")
    @Pattern(regexp = "^[0-9\\s-]+$", message = "Postal code must contain only numbers, spaces, or hyphens")
    private String postalCode;

    private String state; // Added state field

    // Getters and Setters
    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getStreetAddress() {
        return streetAddress;
    }

    public void setStreetAddress(String streetAddress) {
        this.streetAddress = streetAddress;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getPostalCode() {
        return postalCode;
    }

    public void setPostalCode(String postalCode) {
        this.postalCode = postalCode;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }
}