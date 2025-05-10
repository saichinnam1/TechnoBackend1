package com.ecommerce.ecommerce_backend.Controller;

import com.ecommerce.ecommerce_backend.entity.Product;
import com.ecommerce.ecommerce_backend.repository.ProductRepository;
import okhttp3.*;
import okhttp3.RequestBody;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private static final Logger logger = LoggerFactory.getLogger(ProductController.class);
    private static final String FREEIMAGE_API_KEY = "6d207e02198a847aa98d0a2a901485"; // Updated API key

    private final ProductRepository productRepository;
    private final OkHttpClient client;

    public ProductController(ProductRepository productRepository) {
        this.productRepository = productRepository;
        this.client = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .writeTimeout(10, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build();
    }

    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @PostMapping("/upload")
    public ResponseEntity<?> addProductWithImage(@RequestParam("file") MultipartFile file,
                                                 @RequestParam("name") String name,
                                                 @RequestParam("description") String description,
                                                 @RequestParam("price") double price,
                                                 @RequestParam("category") String category) {
        try {
            // Input validation
            if (file.isEmpty()) return ResponseEntity.badRequest().body("Please select a file to upload.");
            if (!file.getContentType().startsWith("image/")) {
                return ResponseEntity.badRequest().body("Only image files are allowed.");
            }
            if (file.getSize() > 10 * 1024 * 1024) { // 10MB limit
                return ResponseEntity.badRequest().body("File size exceeds 10MB limit.");
            }
            if (name == null || name.trim().isEmpty()) return ResponseEntity.badRequest().body("Product name is required.");
            if (description == null || description.trim().isEmpty()) return ResponseEntity.badRequest().body("Description is required.");
            if (price <= 0) return ResponseEntity.badRequest().body("Price must be greater than 0.");
            if (category == null || category.trim().isEmpty()) return ResponseEntity.badRequest().body("Category is required.");

            logger.info("Uploading file: {} (size: {} bytes)", file.getOriginalFilename(), file.getSize());

            // Upload file to FreeImage.host
            String imageUrl = uploadToFreeImageHost(file);
            if (imageUrl == null) {
                logger.warn("Falling back to saving product without image due to FreeImage.host failure.");
            }

            // Save product
            Product product = new Product();
            product.setName(name);
            product.setDescription(description);
            product.setPrice(price);
            product.setCategory(category);
            product.setImageUrl(imageUrl); // Will be null if upload failed
            Product savedProduct;
            try {
                savedProduct = productRepository.save(product);
            } catch (Exception e) {
                logger.error("Failed to save product to database: {}", e.getMessage(), e);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to save product to database: " + e.getMessage());
            }

            logger.info("Product saved: {}", savedProduct.getName());
            return ResponseEntity.ok(convertToResponse(savedProduct));

        } catch (Exception e) {
            logger.error("Error saving product: {}", e.getMessage(), e);
            if (e.getMessage() != null && e.getMessage().contains("FreeImage.host")) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to upload image to FreeImage.host: " + e.getMessage());
            }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error saving product: " + e.getMessage());
        }
    }

    private String uploadToFreeImageHost(MultipartFile file) throws Exception {
        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("source", file.getOriginalFilename(),
                        RequestBody.create(file.getBytes(), MediaType.parse("image/*")))
                .addFormDataPart("type", "file")
                .addFormDataPart("key", FREEIMAGE_API_KEY)
                .build();

        Request request = new Request.Builder()
                .url("https://freeimage.host/api/1/upload")
                .post(requestBody)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                logger.error("Failed to upload image to FreeImage.host: {} - {}", response.code(), response.message());
                return null;
            }

            String responseBody = response.body().string();
            logger.info("FreeImage.host response: {}", responseBody);
            ObjectMapper mapper = new ObjectMapper();
            JsonNode rootNode = mapper.readTree(responseBody);
            String imageUrl = rootNode.path("image").path("url").asText();
            if (imageUrl == null || imageUrl.isEmpty()) {
                logger.error("Failed to parse image URL from FreeImage.host response");
                return null;
            }
            return imageUrl;
        } catch (Exception e) {
            logger.error("Exception during FreeImage.host upload: {}", e.getMessage(), e);
            throw e; // Let the outer catch block handle it
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
        Map<String, Object> response = new java.util.HashMap<>();
        response.put("id", product.getId());
        response.put("name", product.getName());
        response.put("description", product.getDescription());
        response.put("price", product.getPrice());
        response.put("category", product.getCategory());
        response.put("imageUrl", product.getImageUrl() != null ? product.getImageUrl() : null);
        return response;
    }
}
