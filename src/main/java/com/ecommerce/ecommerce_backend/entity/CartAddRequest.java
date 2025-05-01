package com.ecommerce.ecommerce_backend.entity;


import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class CartAddRequest {
    @NotNull(message = "User ID must not be null")
    private Long userId;

    @NotNull(message = "Product ID must not be null")
    private Long productId;

    @NotNull(message = "Quantity must not be null")
    @Min(value = 1, message = "Quantity must be at least 1")
    private Integer quantity;

    public CartAddRequest() {
    }

    public CartAddRequest(Long userId, Long productId, Integer quantity) {
        this.userId = userId;
        this.productId = productId;
        this.quantity = quantity;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }
}