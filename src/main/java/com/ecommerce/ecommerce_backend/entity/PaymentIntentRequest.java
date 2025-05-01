package com.ecommerce.ecommerce_backend.entity;



import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public class PaymentIntentRequest {
    @NotNull(message = "User ID must not be null")
    private Long userId;

    @NotNull(message = "Amount must not be null")
    @Min(value = 1, message = "Amount must be greater than 0")
    private Double amount;

    @NotNull(message = "Currency must not be null")
    private String currency;

    @NotEmpty(message = "Items must not be empty")
    private List<Item> items;

    public static class Item {
        @NotNull(message = "Product ID must not be null")
        private Long productId;

        @NotNull(message = "Quantity must not be null")
        @Min(value = 1, message = "Quantity must be at least 1")
        private Integer quantity;

        @NotNull(message = "Price must not be null")
        @Min(value = 0, message = "Price must be greater than or equal to 0")
        private Double price;

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

        public Double getPrice() {
            return price;
        }

        public void setPrice(Double price) {
            this.price = price;
        }
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Double getAmount() {
        return amount;
    }

    public void setAmount(Double amount) {
        this.amount = amount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public List<Item> getItems() {
        return items;
    }

    public void setItems(List<Item> items) {
        this.items = items;
    }
}