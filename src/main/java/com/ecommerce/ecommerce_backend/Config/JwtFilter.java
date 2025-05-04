package com.ecommerce.ecommerce_backend.Config;

import com.ecommerce.ecommerce_backend.util.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class JwtFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(JwtFilter.class);
    private static final String TOKEN_PREFIX = "Bearer ";

    private final JwtUtil jwtUtil;

    public JwtFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String requestURI = request.getRequestURI();
        logger.debug("Processing request for URI: {}", requestURI);

        // Skip filtering for frontend routes and permitted API endpoints
        if (!requestURI.startsWith("/api/") || // Skip non-API routes (e.g., /auth/reset)
                requestURI.startsWith("/api/auth/") ||
                requestURI.startsWith("/api/oauth2/") ||
                requestURI.startsWith("/login/") ||
                requestURI.startsWith("/oauth2/") ||
                requestURI.startsWith("/login/oauth2/code/") ||
                requestURI.equals("/favicon.ico") ||
                requestURI.startsWith("/accounts/") ||
                requestURI.startsWith("/api/reset-password/")) {
            logger.info("Skipping JWT filter for URI: {}", requestURI);
            chain.doFilter(request, response);
            return;
        }

        String authorizationHeader = request.getHeader("Authorization");

        if (authorizationHeader == null || !authorizationHeader.startsWith(TOKEN_PREFIX)) {
            logger.debug("No Bearer token found in Authorization header for URI: {}", requestURI);
            chain.doFilter(request, response);
            return;
        }

        String token = authorizationHeader.substring(TOKEN_PREFIX.length());
        String username;
        try {
            username = jwtUtil.extractUsername(token);
            if (username == null) {
                logger.warn("Could not extract username from token for URI: {}", requestURI);
                chain.doFilter(request, response);
                return;
            }
            logger.debug("Extracted username: {} for URI: {}", username, requestURI);
        } catch (Exception e) {
            logger.error("Error extracting username from token for URI {}: {}", requestURI, e.getMessage());
            chain.doFilter(request, response);
            return;
        }

        if (SecurityContextHolder.getContext().getAuthentication() == null && jwtUtil.validateToken(token, username)) {
            Set<String> roles = jwtUtil.extractRoles(token);
            Set<SimpleGrantedAuthority> authorities = roles.stream()
                    .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                    .collect(Collectors.toSet());

            UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                    username, null, authorities
            );
            authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authToken);
            logger.debug("Authentication set for user: {} with roles: {} for URI: {}", username, roles, requestURI);
        } else {
            logger.warn("Token validation failed for user: {}. Expired: {} for URI: {}", username, jwtUtil.isTokenExpired(token), requestURI);
        }

        chain.doFilter(request, response);
    }
}