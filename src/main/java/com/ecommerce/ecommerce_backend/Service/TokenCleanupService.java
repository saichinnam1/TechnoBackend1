package com.ecommerce.ecommerce_backend.Service;

import com.ecommerce.ecommerce_backend.repository.PasswordResetTokenRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@EnableScheduling
public class TokenCleanupService {

    @Autowired
    private PasswordResetTokenRepository tokenRepository;

    @Scheduled(cron = "0 0 0 * * ?") // Run daily at midnight
    @Transactional
    public void cleanUpExpiredTokens() {
        tokenRepository.deleteAllExpiredTokens(LocalDateTime.now());
    }
}