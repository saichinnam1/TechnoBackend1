package com.ecommerce.ecommerce_backend.Controller;

import com.ecommerce.ecommerce_backend.entity.Product;
import com.ecommerce.ecommerce_backend.repository.ProductRepository;
import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private static final Logger logger = LoggerFactory.getLogger(ProductController.class);

    @Autowired
    private ProductRepository productRepository;

    @Value("${cloudinary.cloud-name}")
    private String cloudName;

    @Value("${cloudinary.api-key}")
    private String apiKey;

    @Value("${cloudinary.api-secret}")
    private String apiSecret;

    private final Cloudinary cloudinary;

    @Autowired
    public ProductController() {
        // Initialize Cloudinary with environment variables (will be set during runtime)
        this.cloudinary = new Cloudinary(ObjectUtils.asMap(
                "cloud_name", System.getenv("CLOUDINARY_CLOUD_NAME") != null ? System.getenv("CLOUDINARY_CLOUD_NAME") : cloudName,
                "api_key", System.getenv("CLOUDINARY_API_KEY") != null ? System.getenv("CLOUDINARY_API_KEY") : apiKey,
                "api_secret", System.getenv("CLOUDINARY_API_SECRET") != null ? System.getenv("CLOUDINARY_API_SECRET") : apiSecret
        ));
    }

    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @PostMapping("/upload")
    public ResponseEntity<?> addProductWithImage(@RequestParam("file") MultipartFile file,
                                                 @RequestParam("name") String name,
                                                 @RequestParam("description") String description,
                                                 @RequestParam("price") double price,
                                                 @RequestParam("category") String category) {
        try {
            if (file.isEmpty()) return ResponseEntity.badRequest().body("Please select a file to upload.");
            if (name == null || name.trim().isEmpty()) return ResponseEntity.badRequest().body("Product name is required.");
            if (description == null || description.trim().isEmpty()) return ResponseEntity.badRequest().body("Description is required.");
            if (price <= 0) return ResponseEntity.badRequest().body("Price must be greater than 0.");
            if (category == null || category.trim().isEmpty()) return ResponseEntity.badRequest().body("Category is required.");

            // Upload file to Cloudinary
            Map uploadResult = cloudinary.uploader().upload(file.getBytes(), ObjectUtils.asMap(
                    "resource_type", "image",
                    "public_id", "products/" + System.currentTimeMillis() + "_" + file.getOriginalFilename()
            ));

            String imageUrl = uploadResult.get("secure_url").toString();
            logger.info("File uploaded to Cloudinary successfully: {}", imageUrl);

            // Save product with image URL
            Product product = new Product();
            product.setName(name);
            product.setDescription(description);
            product.setPrice(price);
            product.setCategory(category);
            product.setImageUrl(imageUrl);
            Product savedProduct = productRepository.save(product);

            logger.info("Product saved: {}", savedProduct.getName());
            return ResponseEntity.ok(savedProduct);

        } catch (IOException e) {
            logger.error("File upload failed: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to upload file: " + e.getMessage());
        } catch (Exception e) {
            logger.error("Error saving product: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error saving product: " + e.getMessage());
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<Product> getProductById(@PathVariable Long id) {
        Optional<Product> product = productRepository.findById(id);
        return product.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).body(null));
    }

    @GetMapping
    public ResponseEntity<List<Product>> getAllProducts() {
        logger.info("Fetching all products");
        try {
            List<Product> products = productRepository.findAll();
            return ResponseEntity.ok(products);
        } catch (Exception e) {
            logger.error("Error fetching products: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Collections.emptyList());
        }
    }

    @GetMapping("/category/{category}")
    public List<Product> getProductsByCategory(@PathVariable String category) {
        return productRepository.findByCategory(category);
    }

    @GetMapping("/search")
    public List<Product> searchProducts(@RequestParam("query") String query) {
        return productRepository.findByNameContainingIgnoreCaseOrDescriptionContainingIgnoreCase(query);
    }
}