package com.ecommerce.ecommerce_backend.entity;



public class CartCleanupResponse {
    private int removedCount;
    private String message;

    public CartCleanupResponse(int removedCount, String message) {
        this.removedCount = removedCount;
        this.message = message;
    }

    public int getRemovedCount() {
        return removedCount;
    }

    public void setRemovedCount(int removedCount) {
        this.removedCount = removedCount;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}