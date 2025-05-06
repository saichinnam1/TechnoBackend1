package com.ecommerce.ecommerce_backend.Controller;

import com.ecommerce.ecommerce_backend.entity.Product;
import com.ecommerce.ecommerce_backend.repository.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private static final Logger logger = LoggerFactory.getLogger(ProductController.class);

    @Autowired
    private ProductRepository productRepository;

    @Value("${aws.s3.bucket}")
    private String bucketName;

    @Value("${aws.region}")
    private String region;

    private final S3Client s3Client;

    @Autowired
    public ProductController(S3Client s3Client) {
        this.s3Client = s3Client;
    }

    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @PostMapping("/upload")
    public ResponseEntity<?> addProductWithImage(
            @RequestParam("file") MultipartFile file,
            @RequestParam("name") String name,
            @RequestParam("description") String description,
            @RequestParam("price") double price,
            @RequestParam("category") String category) {
        try {
            // Input validation
            if (file.isEmpty()) return ResponseEntity.badRequest().body("Please select a file to upload.");
            if (name == null || name.trim().isEmpty()) return ResponseEntity.badRequest().body("Product name is required.");
            if (description == null || description.trim().isEmpty()) return ResponseEntity.badRequest().body("Description is required.");
            if (price <= 0) return ResponseEntity.badRequest().body("Price must be greater than 0.");
            if (category == null || category.trim().isEmpty()) return ResponseEntity.badRequest().body("Category is required.");
            if (bucketName == null || bucketName.trim().isEmpty()) return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("S3 bucket name is not configured.");

            // Generate unique file name
            String fileName = UUID.randomUUID().toString() + "_" + file.getOriginalFilename();
            logger.debug("Generated file name: {}", fileName);

            // Upload to AWS S3 with public-read ACL
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(fileName)
                    .acl("public-read") // Ensure public access
                    .build();
            try {
                s3Client.putObject(putObjectRequest, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
                logger.info("File uploaded to S3 successfully: {} in bucket {}", fileName, bucketName);
            } catch (S3Exception e) {
                logger.error("Failed to upload file to S3: {}", e.getMessage(), e);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to upload file to S3: " + e.getMessage());
            }

            // Generate region-specific URL
            String imageUrl;
            if (region.equals("us-east-1")) {
                imageUrl = String.format("https://%s.s3.amazonaws.com/%s", bucketName, fileName);
            } else {
                imageUrl = String.format("https://%s.s3.%s.amazonaws.com/%s", bucketName, region, fileName);
            }
            logger.debug("Generated image URL: {}", imageUrl);

            // Save product
            Product product = new Product();
            product.setName(name);
            product.setDescription(description);
            product.setPrice(price);
            product.setCategory(category);
            product.setImageUrl(imageUrl);
            Product savedProduct = productRepository.save(product);

            logger.info("Product saved: {} with ID {}", savedProduct.getName(), savedProduct.getId());
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
        logger.debug("Fetching product with ID: {}", id);
        Optional<Product> product = productRepository.findById(id);
        return product.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).body(null));
    }

    @GetMapping
    public ResponseEntity<List<Product>> getAllProducts() {
        logger.info("Fetching all products");
        try {
            List<Product> products = productRepository.findAll();
            logger.debug("Found {} products", products.size());
            return ResponseEntity.ok(products);
        } catch (Exception e) {
            logger.error("Error fetching products: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Collections.emptyList());
        }
    }

    @GetMapping("/category/{category}")
    public List<Product> getProductsByCategory(@PathVariable String category) {
        logger.debug("Fetching products for category: {}", category);
        List<Product> products = productRepository.findByCategory(category);
        logger.debug("Found {} products in category {}", products.size(), category);
        return products;
    }

    @GetMapping("/search")
    public List<Product> searchProducts(@RequestParam("query") String query) {
        logger.debug("Searching products with query: {}", query);
        List<Product> products = productRepository.findByNameContainingIgnoreCaseOrDescriptionContainingIgnoreCase(query);
        logger.debug("Found {} products matching query '{}'", products.size(), query);
        return products;
    }
}