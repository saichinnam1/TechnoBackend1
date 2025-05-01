package com.ecommerce.ecommerce_backend.Config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.File;

@Configuration
public class StaticResourceConfig implements WebMvcConfigurer {
    @Value("${upload.directory:/app/uploads}")
    private String uploadDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String normalizedUploadDir = uploadDir.endsWith(File.separator) ? uploadDir : uploadDir + File.separator;
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + normalizedUploadDir);
    }
}