package com.ecommerce.ecommerce_backend.repository;


import com.ecommerce.ecommerce_backend.entity.ContactMessage;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContactMessageRepository extends JpaRepository<ContactMessage, Long> {
}