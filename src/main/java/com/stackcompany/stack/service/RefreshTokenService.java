package com.stackcompany.stack.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RefreshTokenService {
    
    // In-memory storage: token -> user email
    private final Map<String, TokenInfo> tokenStore = new ConcurrentHashMap<>();
    
    private static class TokenInfo {
        String userEmail;
        LocalDateTime expiresAt;
        
        TokenInfo(String userEmail, LocalDateTime expiresAt) {
            this.userEmail = userEmail;
            this.expiresAt = expiresAt;
        }
    }
    
    /**
     * Store a refresh token in memory
     */
    public void storeToken(String token, String userEmail, LocalDateTime expiresAt) {
        tokenStore.put(token, new TokenInfo(userEmail, expiresAt));
    }
    
    /**
     * Get user email from refresh token
     */
    public String getUserEmail(String token) {
        TokenInfo info = tokenStore.get(token);
        if (info == null) {
            return null;
        }
        
        // Check if token is expired
        if (info.expiresAt.isBefore(LocalDateTime.now())) {
            tokenStore.remove(token);
            return null;
        }
        
        return info.userEmail;
    }
    
    /**
     * Remove a refresh token
     */
    public void removeToken(String token) {
        tokenStore.remove(token);
    }
    
    /**
     * Check if token exists and is valid
     */
    public boolean isValidToken(String token) {
        TokenInfo info = tokenStore.get(token);
        if (info == null) {
            return false;
        }
        
        if (info.expiresAt.isBefore(LocalDateTime.now())) {
            tokenStore.remove(token);
            return false;
        }
        
        return true;
    }
    
    /**
     * Clean up expired tokens periodically (runs every hour)
     */
    @Scheduled(fixedRate = 3600000) // 1 hour in milliseconds
    public void cleanupExpiredTokens() {
        LocalDateTime now = LocalDateTime.now();
        tokenStore.entrySet().removeIf(entry -> entry.getValue().expiresAt.isBefore(now));
    }
}

