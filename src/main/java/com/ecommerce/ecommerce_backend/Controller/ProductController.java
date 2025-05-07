package com.ecommerce.ecommerce_backend.Controller;

import com.ecommerce.ecommerce_backend.entity.Product;
import com.ecommerce.ecommerce_backend.repository.ProductRepository;
import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private static final Logger logger = LoggerFactory.getLogger(ProductController.class);

    private final ProductRepository productRepository;
    private final Cloudinary cloudinary;

    @Value("${cloudinary.cloud-name}")
    private String cloudinaryCloudName;

    public ProductController(ProductRepository productRepository) {
        this.productRepository = productRepository;
        // Initialize Cloudinary with environment variables
        this.cloudinary = new Cloudinary(ObjectUtils.asMap(
                "cloud_name", System.getenv("CLOUDINARY_CLOUD_NAME") != null ? System.getenv("CLOUDINARY_CLOUD_NAME") : cloudinaryCloudName,
                "api_key", System.getenv("CLOUDINARY_API_KEY"),
                "api_secret", System.getenv("CLOUDINARY_API_SECRET")
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

            String publicId = uploadResult.get("public_id").toString(); // e.g., "products/1698123456789_image"
            logger.info("File uploaded to Cloudinary successfully: {}", publicId);

            // Save product with Cloudinary public ID
            Product product = new Product();
            product.setName(name);
            product.setDescription(description);
            product.setPrice(price);
            product.setCategory(category);
            product.setImageUrl(publicId); // Store public ID instead of full URL
            Product savedProduct = productRepository.save(product);

            logger.info("Product saved: {}", savedProduct.getName());
            return ResponseEntity.ok(convertToResponse(savedProduct));

        } catch (IOException e) {
            logger.error("File upload failed: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to upload file: " + e.getMessage());
        } catch (Exception e) {
            logger.error("Error saving product: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error saving product: " + e.getMessage());
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getProductById(@PathVariable Long id) {
        Optional<Product> product = productRepository.findById(id);
        return product.map(p -> ResponseEntity.ok(convertToResponse(p)))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).body(null));
    }

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getAllProducts() {
        logger.info("Fetching all products");
        try {
            List<Product> products = productRepository.findAll();
            List<Map<String, Object>> response = products.stream()
                    .map(this::convertToResponse)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error fetching products: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Collections.emptyList());
        }
    }

    @GetMapping("/category/{category}")
    public List<Map<String, Object>> getProductsByCategory(@PathVariable String category) {
        return productRepository.findByCategory(category).stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @GetMapping("/search")
    public List<Map<String, Object>> searchProducts(@RequestParam("query") String query) {
        return productRepository.findByNameContainingIgnoreCaseOrDescriptionContainingIgnoreCase(query).stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    private Map<String, Object> convertToResponse(Product product) {
        Map<String, Object> response = ObjectUtils.asMap(
                "id", product.getId(),
                "name", product.getName(),
                "description", product.getDescription(),
                "price", product.getPrice(),
                "category", product.getCategory()
        );

        String publicId = product.getImageUrl();
        if (publicId != null && !publicId.isEmpty()) {

            if (publicId.startsWith("https://res.cloudinary.com")) {
                publicId = publicId.split("/v1/")[1].split("/w_")[0]; // Extract public ID from full URL
            }
            String cloudinaryUrl = String.format("https://res.cloudinary.com/%s/image/upload/v1/%s", cloudinaryCloudName, publicId);
            response.put("imageUrl", cloudinaryUrl);
        } else {
            response.put("imageUrl", null);
        }
        return response;
    }
}