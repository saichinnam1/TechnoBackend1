package com.ecommerce.ecommerce_backend.Controller;



import com.ecommerce.ecommerce_backend.entity.ContactMessage;
import com.ecommerce.ecommerce_backend.repository.ContactMessageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/contact")
public class ContactController {

    @Autowired
    private ContactMessageRepository contactMessageRepository;

    @PostMapping
    public ResponseEntity<String> submitContactForm(@RequestBody ContactMessage contactMessage) {
        try {
            contactMessageRepository.save(contactMessage);
            return ResponseEntity.ok("Message submitted successfully!");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to submit message: " + e.getMessage());
        }
    }
}
