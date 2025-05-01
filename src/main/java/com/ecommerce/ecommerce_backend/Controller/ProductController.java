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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/products")
public class ProductController {
    private static final Logger logger = LoggerFactory.getLogger(ProductController.class);

    @Autowired
    private ProductRepository productRepository;

    @Value("${upload.directory:uploads/}")
    private String uploadDir;

    @GetMapping("/category/{category}")
    public List<Product> getProductsByCategory(@PathVariable String category) {
        return productRepository.findByCategory(category);
    }

    @PostMapping
    public Product addProduct(@RequestBody Product product) {
        return productRepository.save(product);
    }

    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @PostMapping("/upload")
    public ResponseEntity<?> addProductWithImage(@RequestParam("file") MultipartFile file,
                                                 @RequestParam("name") String name,
                                                 @RequestParam("description") String description,
                                                 @RequestParam("price") double price,
                                                 @RequestParam("category") String category) {
        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body("Please select a file to upload.");
            }
            if (name == null || name.trim().isEmpty()) {
                return ResponseEntity.badRequest().body("Product name is required.");
            }
            if (description == null || description.trim().isEmpty()) {
                return ResponseEntity.badRequest().body("Description is required.");
            }
            if (price <= 0) {
                return ResponseEntity.badRequest().body("Price must be greater than 0.");
            }
            if (category == null || category.trim().isEmpty()) {
                return ResponseEntity.badRequest().body("Category is required.");
            }

            Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
                logger.info("Created upload directory: {}", uploadPath);
            }

            if (!Files.isWritable(uploadPath)) {
                logger.error("Upload directory is not writable: {}", uploadPath);
                return ResponseEntity.status(500).body("Upload directory is not writable.");
            }

            String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();
            Path dest = uploadPath.resolve(fileName);
            file.transferTo(dest.toFile());
            if (!Files.exists(dest)) {
                logger.error("File does not exist after upload: {}", dest);
                return ResponseEntity.status(500).body("File upload failed: File not found after upload");
            }
            logger.info("File uploaded successfully: {}", fileName);

            Product product = new Product();
            product.setName(name);
            product.setDescription(description);
            product.setPrice(price);
            product.setCategory(category);
            product.setImageUrl("/uploads/" + fileName); // Store relative path
            Product savedProduct = productRepository.save(product);
            logger.info("Product saved successfully: {}", savedProduct.getName());
            return ResponseEntity.ok(savedProduct);
        } catch (IOException e) {
            logger.error("File upload failed: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body("Failed to upload file: " + e.getMessage());
        } catch (Exception e) {
            logger.error("Error saving product: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body("Error saving product: " + e.getMessage());
        }
    }
    @GetMapping("/{id}")
    public ResponseEntity<Product> getProductById(@PathVariable Long id) {
        Optional<Product> product = productRepository.findById(id);
        return product.isPresent()
                ? ResponseEntity.ok(product.get())
                : ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
    }

    @GetMapping
    public ResponseEntity<List<Product>> getAllProducts() {
        logger.info("Fetching all products");
        try {
            List<Product> products = productRepository.findAll();
            logger.debug("Fetched {} products", products.size());
            return ResponseEntity.ok(products);
        } catch (Exception e) {
            logger.error("Error fetching products: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Collections.emptyList());
        }
    }

    @GetMapping("/search")
    public List<Product> searchProducts(@RequestParam("query") String query) {
        return productRepository.findByNameContainingIgnoreCaseOrDescriptionContainingIgnoreCase(query);
    }
}