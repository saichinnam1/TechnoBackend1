package com.ecommerce.ecommerce_backend.Controller;

import com.ecommerce.ecommerce_backend.Service.PasswordResetService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/reset-password")
public class PasswordResetController {

    private static final Logger logger = LoggerFactory.getLogger(PasswordResetController.class);

    @Autowired
    private PasswordResetService passwordResetService;

    @PostMapping
    public ResponseEntity<String> createPasswordResetToken(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        if (email == null || email.isEmpty()) {
            return ResponseEntity.badRequest().body("Email is required");
        }
        try {
            passwordResetService.createPasswordResetTokenForUser(email);
            return ResponseEntity.ok("Password reset email sent successfully");
        } catch (Exception e) {
            logger.error("Failed to process password reset request for email: {}. Error: {}", email, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to process password reset request: " + e.getMessage());
        }
    }

    @GetMapping("/validate/{token}")
    public ResponseEntity<String> validatePasswordResetToken(@PathVariable String token) {
        try {
            boolean isValid = passwordResetService.validatePasswordResetToken(token);
            return ResponseEntity.ok(isValid ? "Token is valid" : "Token is invalid or expired");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            logger.error("Failed to validate token: {}. Error: {}", token, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to validate token: " + e.getMessage());
        }
    }

    @PostMapping("/reset")
    public ResponseEntity<String> resetPassword(@RequestBody Map<String, String> request) {
        String token = request.get("token");
        String newPassword = request.get("newPassword");
        if (token == null || newPassword == null) {
            return ResponseEntity.badRequest().body("Token and new password are required");
        }
        try {
            passwordResetService.resetPassword(token, newPassword);
            return ResponseEntity.ok("Password reset successfully");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            logger.error("Failed to reset password for token: {}. Error: {}", token, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to reset password: " + e.getMessage());
        }
    }
}