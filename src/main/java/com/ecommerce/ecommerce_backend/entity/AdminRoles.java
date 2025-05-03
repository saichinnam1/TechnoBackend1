package com.ecommerce.ecommerce_backend.entity;

import jakarta.persistence.*;

import java.util.Set;

@Entity
public class AdminRoles {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "admin_id", nullable = false)
    private Admin admin;
}

