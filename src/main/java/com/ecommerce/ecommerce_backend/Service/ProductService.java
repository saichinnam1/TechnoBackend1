package com.ecommerce.ecommerce_backend.Service;



import com.ecommerce.ecommerce_backend.entity.Product;
import com.ecommerce.ecommerce_backend.entity.Product;
import com.ecommerce.ecommerce_backend.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ProductService {

    private final ProductRepository productRepository;

    @Value("${cloudinary.cloud-name}")
    private String cloudinaryCloudName;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public List<Product> getAllProducts() {
        List<Product> products = productRepository.findAll();
        return products.stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    public Product getProductById(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + id));
        return convertToDTO(product);
    }

    private Product convertToDTO(Product product) {
        Product dto = new Product();
        dto.setId(product.getId());
        dto.setName(product.getName());
        dto.setPrice(product.getPrice());
        dto.setCategory(product.getCategory());
        dto.setDescription(product.getDescription());

        // Construct Cloudinary URL from the public ID stored in imageUrl
        String publicId = product.getImageUrl(); // e.g., "products/product1"
        if (publicId != null && !publicId.isEmpty()) {
            // Remove any .jpg extension if present
            publicId = publicId.replace(".jpg", "");
            String cloudinaryUrl = String.format("https://res.cloudinary.com/%s/image/upload/v1/%s", cloudinaryCloudName, publicId);
            dto.setImageUrl(cloudinaryUrl);
        } else {
            dto.setImageUrl(null); // Or set a default placeholder image URL
        }

        return dto;
    }
}