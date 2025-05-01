package com.ecommerce.ecommerce_backend.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonBackReference;

@Entity
@Table(name = "order_items")
public class OrderItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "order_id", nullable = false)
    @NotNull(message = "Order must not be null")
    @JsonBackReference
    private CustomerOrder order;




    @ManyToOne
    @JoinColumn(name = "product_id", nullable = false) // Ensure product_id cannot be null in the database
    @NotNull(message = "Product must not be null")
    private Product product;

    @Column(nullable = false) // Ensure price cannot be null in the database
    @NotNull(message = "Price must not be null")
    @Min(value = 0, message = "Price must be greater than or equal to 0")
    private double price;

    @Column(nullable = false) // Ensure quantity cannot be null in the database
    @NotNull(message = "Quantity must not be null")
    @Min(value = 1, message = "Quantity must be at least 1")
    private int quantity;

    // Default constructor
    public OrderItem() {
    }

    // Parameterized constructor for convenience
    public OrderItem(CustomerOrder order, Product product, double price, int quantity) {
        this.order = order;
        this.product = product;
        this.price = price;
        this.quantity = quantity;
    }

    // Getters and setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public CustomerOrder getOrder() {
        return order;
    }

    public void setOrder(CustomerOrder order) {
        this.order = order;
    }

    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        this.product = product;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    // toString method for better logging and debugging
    @Override
    public String toString() {
        return "OrderItem{" +
                "id=" + id +
                ", orderId=" + (order != null ? order.getId() : "null") +
                ", productId=" + (product != null ? product.getId() : "null") +
                ", price=" + price +
                ", quantity=" + quantity +
                '}';
    }
}