package com.ecommerce.ecommerce_backend.repository;

import com.ecommerce.ecommerce_backend.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long> {
    List<Product> findByCategory(String category);

        @Query("SELECT p FROM Product p WHERE UPPER(p.name) LIKE UPPER(CONCAT('%', ?1, '%')) OR p.description LIKE CONCAT('%', ?1, '%')")
        List<Product> findByNameContainingIgnoreCaseOrDescriptionContainingIgnoreCase(String query);
}
