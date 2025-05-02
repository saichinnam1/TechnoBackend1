package com.ecommerce.ecommerce_backend.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Component
public class JwtUtil {

    private static final Logger logger = LoggerFactory.getLogger(JwtUtil.class);
    private static final String ROLES_CLAIM = "roles";

    @Value("${jwt.secret:/KKpyW07YkFQpwFS13ZT18cQBtmnvjrXhX+n4rHfzO0=}")
    private String secretKey;

    @Value("${jwt.expiration:604800000}") // 7 days for access token
    private long accessTokenExpirationTime;

    @Value("${jwt.refresh.expiration:1209600000}") // 14 days for refresh token
    private long refreshTokenExpirationTime;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private SecretKey key;

    @PostConstruct
    public void init() {
        logger.info("Initializing JwtUtil with secret key length: {}", secretKey.length());
        if (secretKey == null || secretKey.isEmpty() || secretKey.equals("your-very-secure-secret-key-change-me")) {
            logger.error("JWT secret key is not configured properly");
            throw new IllegalStateException("JWT secret key must be configured in application.properties");
        }
        this.key = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
    }

    public String generateAccessToken(String username, Set<String> roles) {
        logger.debug("Generating access token for username: {} with roles: {}", username, roles);
        return Jwts.builder()
                .claim(ROLES_CLAIM, new ArrayList<>(roles))
                .setSubject(username)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + accessTokenExpirationTime))
                .signWith(key, SignatureAlgorithm.HS512)
                .compact();
    }

    public String generateRefreshToken(String username) {
        logger.debug("Generating refresh token for username: {}", username);
        return Jwts.builder()
                .setSubject(username)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + refreshTokenExpirationTime))
                .signWith(key, SignatureAlgorithm.HS512)
                .compact();
    }

    public String extractUsername(String token) {
        try {
            Claims claims = extractClaims(token);
            return claims.getSubject();
        } catch (Exception e) {
            logger.warn("Failed to extract username from token: {}", e.getMessage());
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    public Set<String> extractRoles(String token) {
        try {
            Claims claims = extractClaims(token);
            Object rolesObj = claims.get(ROLES_CLAIM);
            if (rolesObj instanceof List<?>) {
                return new HashSet<>((List<String>) rolesObj);
            }
            logger.warn("Roles not found or not a list in token");
            return new HashSet<>();
        } catch (Exception e) {
            logger.warn("Failed to extract roles from token: {}", e.getMessage());
            return new HashSet<>();
        }
    }

    public boolean isTokenExpired(String token) {
        try {
            return extractClaims(token).getExpiration().before(new Date());
        } catch (Exception e) {
            logger.warn("Token expiration check failed, assuming expired: {}", e.getMessage());
            return true;
        }
    }

    public boolean validateToken(String token, String username) {
        try {
            String extractedUsername = extractUsername(token);
            boolean isExpired = isTokenExpired(token);
            boolean isValid = extractedUsername != null && extractedUsername.equals(username) && !isExpired;
            logger.debug("Token validation - Username: {}, Expired: {}, Valid: {}", extractedUsername, isExpired, isValid);
            return isValid;
        } catch (Exception e) {
            logger.warn("Token validation failed for username {}: {}", username, e.getMessage());
            return false;
        }
    }

    public String toJson(Object object) {
        try {
            return objectMapper.writeValueAsString(object);
        } catch (Exception e) {
            logger.error("Failed to serialize object to JSON: {}", e.getMessage());
            throw new RuntimeException("Failed to serialize object to JSON", e);
        }
    }

    private Claims extractClaims(String token) {
        try {
            return Jwts.parser()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (Exception e) {
            logger.error("Error extracting claims from token: {}", e.getMessage());
            throw new IllegalArgumentException("Invalid token", e);
        }
    }
}