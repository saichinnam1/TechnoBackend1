package com.ecommerce.ecommerce_backend.entity;

import jakarta.validation.constraints.NotNull;

public class CartUpdateRequest {
    @NotNull(message = "User ID must not be null")
    private Long userId;

    @NotNull(message = "Item ID must not be null")
    private Long itemId;

    @NotNull(message = "Quantity must not be null")
    private Integer quantity;

    public CartUpdateRequest() {
    }

    public CartUpdateRequest(Long userId, Long itemId, Integer quantity) {
        this.userId = userId;
        this.itemId = itemId;
        this.quantity = quantity;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getItemId() {
        return itemId;
    }

    public void setItemId(Long itemId) {
        this.itemId = itemId;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }
}