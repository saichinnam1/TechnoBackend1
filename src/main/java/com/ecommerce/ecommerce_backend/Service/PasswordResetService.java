package com.ecommerce.ecommerce_backend.Service;

import com.ecommerce.ecommerce_backend.entity.PasswordResetToken;
import com.ecommerce.ecommerce_backend.entity.User;
import com.ecommerce.ecommerce_backend.repository.PasswordResetTokenRepository;
import com.ecommerce.ecommerce_backend.repository.UserRepository;
import org.slf4j.Logger;

import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.mail.MessagingException;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
public class PasswordResetService {

    private static final Logger logger = LoggerFactory.getLogger(PasswordResetService.class);
    private static final int TOKEN_VALIDITY_HOURS = 1;

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;

    public PasswordResetService(UserRepository userRepository,
                                PasswordResetTokenRepository tokenRepository,
                                EmailService emailService,
                                PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
        this.emailService = emailService;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public void createPasswordResetTokenForUser(String email) {
        User user = userRepository.findByEmail(email.toLowerCase())
                .orElseThrow(() -> new IllegalArgumentException("User not found with email: " + email));

        Optional<PasswordResetToken> existingToken = tokenRepository.findByUser(user);
        if (existingToken.isPresent()) {
            tokenRepository.deleteByUser(user);
            logger.info("Deleted existing token for user: {} with id: {}", user.getEmail(), existingToken.get().getId());
            tokenRepository.flush();
        } else {
            logger.info("No existing token found for user: {}", user.getEmail());
        }

        String token = UUID.randomUUID().toString();
        PasswordResetToken resetToken = new PasswordResetToken(token, user);
        resetToken = tokenRepository.save(resetToken);
        logger.info("Created password reset token: {}", resetToken);
        tokenRepository.flush();

        String resetLink = "https://ecommerce-frontend-hf4x.vercel.app/auth/reset/" + token;
        String subject = "Password Reset Request";
        String body = "Hello " + user.getUsername() + ",\n\n" +
                "You have requested to reset your password. Click the link below to reset it:\n" +
                resetLink + "\n\n" +
                "This link is valid for " + TOKEN_VALIDITY_HOURS + " hour(s). If you did not request a password reset, please ignore this email.\n\n" +
                "Best regards,\nThe Ecommerce Team";

        try {
            emailService.sendEmail(user.getEmail(), subject, body);
            logger.info("Password reset email sent successfully to: {}", user.getEmail());
        } catch (MessagingException e) {
            logger.error("Failed to send password reset email to: {}. Error: {}", user.getEmail(), e.getMessage(), e);
            throw new RuntimeException("Failed to send password reset email: " + e.getMessage(), e);
        }
    }

    public boolean validatePasswordResetToken(String token) {
        PasswordResetToken resetToken = tokenRepository.findByToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Invalid token"));

        if (resetToken.isExpired()) {
            logger.warn("Password reset token expired: {}", resetToken);
            throw new IllegalArgumentException("Token has expired");
        }

        logger.info("Password reset token validated successfully: {}", resetToken);
        return true;
    }

    @Transactional
    public void resetPassword(String token, String newPassword) {
        PasswordResetToken resetToken = tokenRepository.findByToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Invalid token"));

        if (resetToken.isExpired()) {
            logger.warn("Password reset token expired: {}", resetToken);
            throw new IllegalArgumentException("Token has expired");
        }

        User user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        tokenRepository.delete(resetToken);
        logger.info("Password reset successful for user: {}. Token deleted: {}", user.getUsername(), resetToken);
    }
}