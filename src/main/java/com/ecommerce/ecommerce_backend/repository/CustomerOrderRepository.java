package com.ecommerce.ecommerce_backend.repository;

import com.ecommerce.ecommerce_backend.entity.CustomerOrder;
import com.ecommerce.ecommerce_backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CustomerOrderRepository extends JpaRepository<CustomerOrder, Long> {
    List<CustomerOrder> findByUser(User user);

    @Query("SELECT co FROM CustomerOrder co WHERE co.user.id = :userId")
    List<CustomerOrder> findByUserId(@Param("userId") Long userId);

    Optional<CustomerOrder> findByPaymentIntentId(String paymentIntentId);

}