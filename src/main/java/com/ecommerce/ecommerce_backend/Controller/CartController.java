package com.ecommerce.ecommerce_backend.Controller;

import com.ecommerce.ecommerce_backend.entity.CartAddRequest;
import com.ecommerce.ecommerce_backend.entity.CartUpdateRequest;
import com.ecommerce.ecommerce_backend.entity.CartResponse;
import com.ecommerce.ecommerce_backend.entity.Cart;
import com.ecommerce.ecommerce_backend.entity.Product;
import com.ecommerce.ecommerce_backend.entity.User;
import com.ecommerce.ecommerce_backend.repository.CartRepository;
import com.ecommerce.ecommerce_backend.repository.ProductRepository;
import com.ecommerce.ecommerce_backend.repository.UserRepository;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/cart")
public class CartController {

    private static final Logger logger = LoggerFactory.getLogger(CartController.class);

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductRepository productRepository;

    @PostMapping("/add")
    public ResponseEntity<?> addToCart(@Valid @RequestBody CartAddRequest request) {
        try {
            Long userId = request.getUserId();
            Long productId = request.getProductId();
            Integer quantity = request.getQuantity();

            if (userId == null || productId == null || quantity == null || quantity <= 0) {
                throw new IllegalArgumentException("User ID, Product ID, and positive quantity are required");
            }

            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found with ID: " + userId));
            Product product = productRepository.findById(productId)
                    .orElseThrow(() -> new RuntimeException("Product not found with ID: " + productId));

            Optional<Cart> existingCart = cartRepository.findByUserAndProduct(user, product);
            Cart cart = existingCart.orElse(new Cart());
            cart.setUser(user);
            cart.setProduct(product);
            cart.setQuantity(existingCart.isPresent() ? cart.getQuantity() + quantity : quantity);

            Cart savedCart = cartRepository.save(cart);
            logger.info("Cart saved successfully: ID {}", savedCart.getId());
            return ResponseEntity.ok(savedCart);
        } catch (IllegalArgumentException e) {
            logger.error("Validation error in addToCart: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (RuntimeException e) {
            logger.error("Runtime exception in addToCart: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (Exception e) {
            logger.error("Unexpected error in addToCart: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error adding to cart: " + e.getMessage());
        }
    }

    @PutMapping("/update")
    public ResponseEntity<?> updateCart(@Valid @RequestBody CartUpdateRequest request) {
        try {
            Long userId = request.getUserId();
            Long itemId = request.getItemId();
            Integer quantity = request.getQuantity();

            if (userId == null || itemId == null || quantity == null || quantity < 0) {
                throw new IllegalArgumentException("User ID, Item ID, and non-negative quantity are required");
            }

            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found with ID: " + userId));
            Cart cart = cartRepository.findById(itemId)
                    .orElseThrow(() -> new RuntimeException("Cart item not found with ID: " + itemId));

            if (!cart.getUser().getId().equals(userId)) {
                throw new RuntimeException("Unauthorized: User does not own this cart item");
            }

            if (quantity <= 0) {
                cartRepository.delete(cart);
                logger.info("Removed cart item with ID: {} due to quantity <= 0", itemId);
                return ResponseEntity.ok(Map.of("success", true, "message", "Cart item removed"));
            }

            cart.setQuantity(quantity);
            Cart updatedCart = cartRepository.save(cart);
            return ResponseEntity.ok(updatedCart);
        } catch (IllegalArgumentException e) {
            logger.error("Validation error updating cart: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (RuntimeException e) {
            logger.error("Runtime error updating cart: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (Exception e) {
            logger.error("Unexpected error updating cart: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Unexpected error updating cart: " + e.getMessage());
        }
    }

    @DeleteMapping("/remove/{itemId}")
    public ResponseEntity<Void> removeFromCart(@PathVariable Long itemId, @RequestParam Long userId) {
        try {
            if (userId == null || itemId == null) {
                throw new IllegalArgumentException("User ID and Item ID are required");
            }

            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            Cart cart = cartRepository.findById(itemId)
                    .orElseThrow(() -> new RuntimeException("Cart item not found"));

            if (!cart.getUser().getId().equals(userId)) {
                throw new RuntimeException("Unauthorized");
            }

            cartRepository.delete(cart);
            logger.info("Removed cart item with ID: {}", itemId);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            logger.error("Validation error removing cart item: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        } catch (RuntimeException e) {
            logger.error("Runtime error removing cart item: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        } catch (Exception e) {
            logger.error("Unexpected error removing cart item: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/{userId}")
    public ResponseEntity<CartResponse> getCart(@PathVariable Long userId) {
        try {
            if (userId == null) {
                throw new IllegalArgumentException("User ID is required");
            }

            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            List<Cart> items = cartRepository.findByUser(user);
            double total = items.stream().mapToDouble(item -> {
                Product product = item.getProduct();
                double price = product != null ? product.getPrice() : 0.0; // No need to check getPrice() for null since it's a primitive double
                int qty = item.getQuantity();
                return price * qty;
            }).sum();

            return ResponseEntity.ok(new CartResponse(items, total));
        } catch (IllegalArgumentException e) {
            logger.error("Validation error getting cart: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        } catch (RuntimeException e) {
            logger.error("Runtime error getting cart: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (Exception e) {
            logger.error("Unexpected error getting cart: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}