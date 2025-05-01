package com.ecommerce.ecommerce_backend.Service;



import com.ecommerce.ecommerce_backend.entity.*;
import com.ecommerce.ecommerce_backend.repository.CustomerOrderRepository;
import com.ecommerce.ecommerce_backend.repository.ProductRepository;
import com.ecommerce.ecommerce_backend.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.mail.MessagingException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class OrderService {

    private static final Logger logger = LoggerFactory.getLogger(OrderService.class);

    @Autowired
    private CustomerOrderRepository orderRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private EmailService emailService;

    @Transactional
    public CustomerOrder placeOrder(Long userId, List<Map<String, Object>> cartItems, String paymentIntentId, Map<String, String> shippingAddressDetails) {
        logger.info("Placing order for user ID: {}", userId);

        // Fetch user
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + userId));

        // Create order
        CustomerOrder order = new CustomerOrder();
        order.setUser(user);
        order.setOrderDate(LocalDateTime.now());
        order.setStatus("PLACED");
        order.setShipmentStatus("PENDING");
        order.setPaymentIntentId(paymentIntentId);

        // Set shipping address
        ShippingAddress shippingAddress = new ShippingAddress();
        shippingAddress.setFullName(shippingAddressDetails.get("fullName"));
        shippingAddress.setStreetAddress(shippingAddressDetails.get("streetAddress"));
        shippingAddress.setCity(shippingAddressDetails.get("city"));

        shippingAddress.setPostalCode(shippingAddressDetails.get("postalCode"));
        order.setShippingAddress(shippingAddress);

        // Map cart items to order items
        List<OrderItem> orderItems = new ArrayList<>();
        double total = 0.0;
        for (Map<String, Object> cartItem : cartItems) {
            Long productId = Long.parseLong(String.valueOf(cartItem.get("productId")));
            int quantity = Integer.parseInt(String.valueOf(cartItem.get("quantity")));
            double price = Double.parseDouble(String.valueOf(cartItem.get("price")));

            Product product = productRepository.findById(productId)
                    .orElseThrow(() -> new IllegalArgumentException("Product not found with ID: " + productId));

            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(order);
            orderItem.setProduct(product);
            orderItem.setQuantity(quantity);
            orderItem.setPrice(price);
            orderItems.add(orderItem);

            total += price * quantity;
        }

        order.setItems(orderItems);
        order.setTotal(total);

        // Save the order
        order = orderRepository.save(order);
        logger.info("Order placed successfully with ID: {}", order.getId());

        // Send order confirmation email
        try {
            emailService.sendOrderConfirmationEmail(order);
            logger.info("Order confirmation email sent for order ID: {}", order.getId());
        } catch (MessagingException e) {
            logger.error("Failed to send order confirmation email for order ID: {}. Error: {}", order.getId(), e.getMessage(), e);
            // Log the error but don't rollback the order placement
        }

        return order;
    }
}