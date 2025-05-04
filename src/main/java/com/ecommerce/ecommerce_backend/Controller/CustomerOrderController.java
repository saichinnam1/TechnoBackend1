package com.ecommerce.ecommerce_backend.Controller;

import com.ecommerce.ecommerce_backend.entity.*;
import com.ecommerce.ecommerce_backend.repository.CartRepository;
import com.ecommerce.ecommerce_backend.repository.CustomerOrderRepository;
import com.ecommerce.ecommerce_backend.repository.ProductRepository;
import com.ecommerce.ecommerce_backend.repository.UserRepository;
import com.ecommerce.ecommerce_backend.Service.EmailService; // Add EmailService
import com.ecommerce.ecommerce_backend.Service.PaymentService;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/orders")
public class CustomerOrderController {

    private static final Logger logger = LoggerFactory.getLogger(CustomerOrderController.class);

    @Autowired
    private CustomerOrderRepository customerOrderRepository;

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private EmailService emailService; // Inject EmailService

    @PostMapping("/create-payment-intent")
    public ResponseEntity<Map<String, Object>> createPaymentIntent(@Valid @RequestBody PaymentIntentRequest request) {
        try {
            Long userId = request.getUserId();
            Double amount = request.getAmount();
            String currency = request.getCurrency();
            List<PaymentIntentRequest.Item> items = request.getItems();

            logger.info("Creating payment intent - UserId: {}, Amount: {}, Items: {}", userId, amount, items);

            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found with ID: " + userId));

            double amountInDollars = amount / 100.0;

            double calculatedTotal = 0.0;
            if (items != null && !items.isEmpty()) {
                calculatedTotal = items.stream()
                        .mapToDouble(item -> {
                            Long prodId = item.getProductId();
                            Integer qty = item.getQuantity();
                            Double priceInCents = item.getPrice();
                            Double priceInDollars = priceInCents / 100.0;
                            Double productPrice = productRepository.findById(prodId)
                                    .map(product -> product.getPrice())
                                    .orElseThrow(() -> new RuntimeException("Product not found with ID: " + prodId));
                            if (Math.abs(productPrice - priceInDollars) > 0.01) {
                                throw new IllegalArgumentException("Price mismatch for product ID " + prodId + ": expected $" + productPrice + ", got $" + priceInDollars);
                            }
                            return productPrice * qty;
                        })
                        .sum();
            }

            if (Math.abs(calculatedTotal - amountInDollars) > 0.01) {
                throw new IllegalArgumentException("Amount mismatch: expected $" + calculatedTotal + ", got $" + amountInDollars);
            }

            long stripeAmount = Math.round(amount);
            PaymentIntent paymentIntent = paymentService.createPaymentIntent(stripeAmount, currency);
            logger.info("Payment intent created successfully: {}", paymentIntent.getId());

            Map<String, Object> response = new HashMap<>();
            response.put("clientSecret", paymentIntent.getClientSecret());
            response.put("paymentIntentId", paymentIntent.getId());
            return ResponseEntity.ok(response);
        } catch (StripeException e) {
            logger.error("Stripe error: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Stripe error: " + e.getMessage(), "details", e.getCode()));
        } catch (IllegalArgumentException e) {
            logger.warn("Validation error: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            logger.error("Internal server error: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Internal server error", "details", e.getMessage()));
        }
    }

    @PostMapping("/checkout")
    @Transactional
    public ResponseEntity<?> checkout(@Valid @RequestBody CheckoutRequest request) {
        try {
            Long userId = request.getUserId();
            String paymentIntentId = request.getPaymentIntentId();
            ShippingAddress shippingAddress = request.getShippingAddress();
            List<Map<String, Object>> cartItems = request.getCartItems();

            logger.info("Processing checkout - UserId: {}, PaymentIntentId: {}, CartItems: {}", userId, paymentIntentId, cartItems);

            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found with ID: " + userId));

            // Retry checking payment intent status
            PaymentIntent paymentIntent = null;
            int maxRetries = 3;
            int retryDelayMs = 1000;
            for (int i = 0; i < maxRetries; i++) {
                paymentIntent = PaymentIntent.retrieve(paymentIntentId);
                if ("succeeded".equals(paymentIntent.getStatus())) {
                    break;
                }
                logger.warn("Payment intent status not succeeded yet: {}. Retrying {}/{}", paymentIntent.getStatus(), i + 1, maxRetries);
                Thread.sleep(retryDelayMs);
            }

            if (!"succeeded".equals(paymentIntent.getStatus())) {
                logger.error("Payment not succeeded after retries: {}", paymentIntent.getStatus());
                return ResponseEntity.status(HttpStatus.PAYMENT_REQUIRED)
                        .body(Map.of("success", false, "message", "Payment failed: " + paymentIntent.getStatus()));
            }

            // Check if an order with the same paymentIntentId already exists
            CustomerOrder existingOrder = customerOrderRepository.findByPaymentIntentId(paymentIntentId).orElse(null);
            CustomerOrder order;

            if (existingOrder != null) {
                // Update existing order
                order = existingOrder;
                order.setShippingAddress(shippingAddress != null ? shippingAddress : new ShippingAddress());
                order.setStatus("PAID");
                order.setShipmentStatus("Packing");
            } else {
                // Create new order
                order = new CustomerOrder();
                order.setUser(user);
                order.setOrderDate(LocalDateTime.now());
                order.setStatus("PAID");
                order.setShipmentStatus("Packing");
                order.setShippingAddress(shippingAddress != null ? shippingAddress : new ShippingAddress());
                order.setPaymentIntentId(paymentIntentId);
            }

            List<OrderItem> orderItems = cartItems.stream()
                    .filter(item -> item.get("productId") != null && item.get("quantity") != null)
                    .map(item -> {
                        Long prodId = Long.valueOf(item.get("productId").toString());
                        Integer qty = Integer.valueOf(item.get("quantity").toString());
                        Double priceInCents = item.get("price") != null ? Double.valueOf(item.get("price").toString()) : 0.0;
                        Double priceInDollars = priceInCents / 100.0;

                        if (qty < 1) {
                            throw new IllegalArgumentException("Quantity must be at least 1 for product ID " + prodId);
                        }

                        Product product = productRepository.findById(prodId)
                                .orElseThrow(() -> new RuntimeException("Product not found with ID: " + prodId));
                        if (Math.abs(product.getPrice() - priceInDollars) > 0.01) {
                            throw new IllegalArgumentException("Price mismatch for product ID " + prodId + ": expected $" + product.getPrice() + ", got $" + priceInDollars);
                        }

                        return new OrderItem(order, product, product.getPrice(), qty);
                    }).collect(Collectors.toList());

            double total = orderItems.stream().mapToDouble(item -> item.getPrice() * item.getQuantity()).sum();
            order.setTotal(total);
            order.setItems(orderItems);

            CustomerOrder savedOrder = customerOrderRepository.save(order);
            cartRepository.deleteAll(cartRepository.findByUser(user));

            // Send order confirmation email
            try {
                emailService.sendOrderConfirmationEmail(savedOrder);
                logger.info("Order confirmation email sent for order ID: {}", savedOrder.getId());
            } catch (Exception e) {
                logger.error("Failed to send order confirmation email for order ID: {}. Error: {}", savedOrder.getId(), e.getMessage(), e);
                // Log the error but don't fail the checkout process
            }

            logger.info("Order processed successfully: ID={}, UserID={}, Items={}", savedOrder.getId(), savedOrder.getUser().getId(), savedOrder.getItems().size());
            return ResponseEntity.ok(Map.of("orderId", savedOrder.getId(), "success", true));
        } catch (StripeException e) {
            logger.error("Stripe error during checkout: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.PAYMENT_REQUIRED)
                    .body(Map.of("success", false, "message", "Payment error: " + e.getMessage(), "details", e.getCode()));
        } catch (IllegalArgumentException e) {
            logger.warn("Validation error during checkout: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        } catch (Exception e) {
            logger.error("Checkout failed: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "Checkout failed", "details", e.getMessage()));
        }
    }

    @GetMapping("/orders/{userId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getOrders(@PathVariable Long userId) {
        try {
            logger.info("Fetching orders for user ID: {}", userId);
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found with ID: " + userId));
            List<CustomerOrder> orders = customerOrderRepository.findByUserId(userId);
            if (orders.isEmpty()) {
                logger.warn("No orders found for user ID: {}", userId);
            } else {
                logger.info("Found {} orders for user ID: {}", orders.size(), userId);
            }
            return ResponseEntity.ok(orders);
        } catch (RuntimeException e) {
            logger.warn("Error fetching orders: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("success", false, "message", e.getMessage()));
        } catch (Exception e) {
            logger.error("Internal server error while fetching orders: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "Internal server error", "details", e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getOrderById(@PathVariable Long id) {
        try {
            logger.info("Fetching order with ID: {}", id);
            CustomerOrder order = customerOrderRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Order not found with ID: " + id));
            return ResponseEntity.ok(order);
        } catch (RuntimeException e) {
            logger.warn("Error fetching order: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("success", false, "message", e.getMessage()));
        } catch (Exception e) {
            logger.error("Internal server error while fetching order: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "Internal server error", "details", e.getMessage()));
        }
    }

    @PutMapping("/{id}/cancel")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> cancelOrder(@PathVariable Long id) {
        try {
            CustomerOrder order = customerOrderRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Order not found with ID: " + id));
            logger.info("Order ID: {}, Shipment Status: {}", id, order.getShipmentStatus());
            if (!"PAID".equalsIgnoreCase(order.getStatus())) {
                return ResponseEntity.badRequest()
                        .body(Map.of("success", false, "message", "Only orders with status 'PAID' can be cancelled."));
            }
            order.setStatus("Cancelled");
            order.setShipmentStatus("Cancelled");
            customerOrderRepository.save(order);
            return ResponseEntity.ok(Map.of("success", true, "message", "Order cancelled successfully"));
        } catch (Exception e) {
            logger.error("Failed to cancel order: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "Failed to cancel order: " + e.getMessage()));
        }
    }
}