package com.ecommerce.ecommerce_backend.entity;



import com.ecommerce.ecommerce_backend.entity.Cart;

import java.util.List;

public class CartResponse {
    private List<Cart> items;
    private double total;

    public CartResponse(List<Cart> items, double total) {
        this.items = items;
        this.total = total;
    }

    public List<Cart> getItems() {
        return items;
    }

    public void setItems(List<Cart> items) {
        this.items = items;
    }

    public double getTotal() {
        return total;
    }

    public void setTotal(double total) {
        this.total = total;
    }
}
