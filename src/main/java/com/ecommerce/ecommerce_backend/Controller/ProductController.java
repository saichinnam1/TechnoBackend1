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

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private static final Logger logger = LoggerFactory.getLogger(ProductController.class);

    @Autowired
    private ProductRepository productRepository;

    @Value("${aws.s3.bucket}")
    private String bucketName;

    private final S3Client s3Client;

    @Autowired
    public ProductController(S3Client s3Client) {
        this.s3Client = s3Client;
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
            if (bucketName == null || bucketName.trim().isEmpty()) return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("S3 bucket name is not configured.");

            String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();

            // Upload to AWS S3
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(fileName)
                    .build();
            try {
                s3Client.putObject(putObjectRequest, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
                logger.info("File uploaded to S3 successfully: {}", fileName);
            } catch (S3Exception e) {
                logger.error("Failed to upload file to S3: {}", e.getMessage(), e);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to upload file to S3: " + e.getMessage());
            }

            String imageUrl = String.format("https://%s.s3.amazonaws.com/%s", bucketName, fileName);

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