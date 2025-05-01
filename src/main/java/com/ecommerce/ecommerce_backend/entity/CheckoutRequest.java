package com.ecommerce.ecommerce_backend.entity;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.Map;

public class CheckoutRequest {
    @NotNull(message = "User ID must not be null")
    private Long userId;

    @NotNull(message = "Payment Intent ID must not be null")
    private String paymentIntentId;

    @NotNull(message = "Shipping address must not be null")
    @Valid
    private ShippingAddress shippingAddress;

    @NotEmpty(message = "Cart items must not be empty")
    private List<Map<String, Object>> cartItems;

    // Getters and Setters
    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getPaymentIntentId() {
        return paymentIntentId;
    }

    public void setPaymentIntentId(String paymentIntentId) {
        this.paymentIntentId = paymentIntentId;
    }

    public ShippingAddress getShippingAddress() {
        return shippingAddress;
    }

    public void setShippingAddress(ShippingAddress shippingAddress) {
        this.shippingAddress = shippingAddress;
    }

    public List<Map<String, Object>> getCartItems() {
        return cartItems;
    }

    public void setCartItems(List<Map<String, Object>> cartItems) {
        this.cartItems = cartItems;
    }
}