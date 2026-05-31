package com.stackcompany.stack.controller;

import com.stackcompany.stack.entity.User;
import com.stackcompany.stack.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "*")
public class UserController {
    
    @Autowired
    private UserRepository userRepository;
    
    @GetMapping("/{id}")
    public ResponseEntity<?> getUserProfile(@PathVariable Long id) {
        try {
            User user = userRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            
            // Build response DTO
            Map<String, Object> response = new HashMap<>();
            response.put("id", user.getId());
            response.put("email", user.getEmail());
            response.put("firstName", user.getFirstName());
            response.put("lastName", user.getLastName());
            response.put("avatarUrl", user.getAvatarUrl());
            response.put("coverImageUrl", user.getCoverImageUrl());
            response.put("gender", user.getGender());
            response.put("headline", user.getHeadline());
            response.put("company", user.getCompany());
            response.put("role", user.getRole());
            response.put("about", user.getAbout());
            response.put("location", user.getLocation());
            response.put("website", user.getWebsite());
            response.put("twitter", user.getTwitter());
            response.put("linkedin", user.getLinkedin());
            response.put("bodivasCompanyCode", user.getBodivasCompanyCode());
            response.put("bodivasCompanyName", user.getBodivasCompanyName());
            response.put("sharesOwned", user.getSharesOwned());
            response.put("sharePercentage", user.getSharePercentage());
            response.put("investmentAmount", user.getInvestmentAmount());
            response.put("currentValue", user.getCurrentValue());
            response.put("averagePrice", user.getAveragePrice());
            
            // Format entry date as string if present
            if (user.getEntryDate() != null) {
                response.put("entryDate", user.getEntryDate().toString());
            }
            
            // Format date of birth as string if present
            if (user.getDateOfBirth() != null) {
                response.put("dateOfBirth", user.getDateOfBirth().toString());
            }
            
            // Format createdAt as ISO string if present
            if (user.getCreatedAt() != null) {
                response.put("createdAt", user.getCreatedAt().toString());
            }
            
            response.put("rule", user.getRule());
            
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponse(e.getMessage()));
        }
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<?> updateUserProfile(
            @PathVariable Long id,
            @RequestBody Map<String, Object> request,
            Authentication authentication) {
        try {
            Long userId = getUserIdFromAuthentication(authentication);
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new ErrorResponse("Authentication required"));
            }
            
            // Check if user is updating their own profile
            if (!userId.equals(id)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(new ErrorResponse("You can only update your own profile"));
            }
            
            User user = userRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            
            // Update fields if provided
            if (request.containsKey("firstName")) {
                user.setFirstName((String) request.get("firstName"));
            }
            if (request.containsKey("lastName")) {
                user.setLastName((String) request.get("lastName"));
            }
            if (request.containsKey("avatarUrl")) {
                user.setAvatarUrl((String) request.get("avatarUrl"));
            }
            if (request.containsKey("coverImageUrl")) {
                user.setCoverImageUrl((String) request.get("coverImageUrl"));
            }
            if (request.containsKey("gender")) {
                user.setGender((String) request.get("gender"));
            }
            if (request.containsKey("headline")) {
                user.setHeadline((String) request.get("headline"));
            }
            if (request.containsKey("company")) {
                user.setCompany((String) request.get("company"));
            }
            if (request.containsKey("role")) {
                user.setRole((String) request.get("role"));
            }
            if (request.containsKey("about")) {
                user.setAbout((String) request.get("about"));
            }
            if (request.containsKey("location")) {
                user.setLocation((String) request.get("location"));
            }
            if (request.containsKey("website")) {
                user.setWebsite((String) request.get("website"));
            }
            if (request.containsKey("twitter")) {
                user.setTwitter((String) request.get("twitter"));
            }
            if (request.containsKey("linkedin")) {
                user.setLinkedin((String) request.get("linkedin"));
            }
            if (request.containsKey("bodivasCompanyCode")) {
                user.setBodivasCompanyCode((String) request.get("bodivasCompanyCode"));
            }
            if (request.containsKey("bodivasCompanyName")) {
                user.setBodivasCompanyName((String) request.get("bodivasCompanyName"));
            }
            if (request.containsKey("sharesOwned")) {
                Object sharesValue = request.get("sharesOwned");
                if (sharesValue != null) {
                    if (sharesValue instanceof Number) {
                        user.setSharesOwned(((Number) sharesValue).longValue());
                    } else if (sharesValue instanceof String) {
                        try {
                            user.setSharesOwned(Long.parseLong((String) sharesValue));
                        } catch (NumberFormatException e) {
                            // Ignore invalid number
                        }
                    }
                }
            }
            if (request.containsKey("sharePercentage")) {
                Object percentageValue = request.get("sharePercentage");
                if (percentageValue != null) {
                    if (percentageValue instanceof Number) {
                        user.setSharePercentage(java.math.BigDecimal.valueOf(((Number) percentageValue).doubleValue()));
                    } else if (percentageValue instanceof String) {
                        try {
                            user.setSharePercentage(new java.math.BigDecimal((String) percentageValue));
                        } catch (NumberFormatException e) {
                            // Ignore invalid number
                        }
                    }
                }
            }
            if (request.containsKey("investmentAmount")) {
                Object amountValue = request.get("investmentAmount");
                if (amountValue != null) {
                    if (amountValue instanceof Number) {
                        user.setInvestmentAmount(java.math.BigDecimal.valueOf(((Number) amountValue).doubleValue()));
                    } else if (amountValue instanceof String) {
                        try {
                            user.setInvestmentAmount(new java.math.BigDecimal((String) amountValue));
                        } catch (NumberFormatException e) {
                            // Ignore invalid number
                        }
                    }
                }
            }
            if (request.containsKey("currentValue")) {
                Object value = request.get("currentValue");
                if (value != null) {
                    if (value instanceof Number) {
                        user.setCurrentValue(java.math.BigDecimal.valueOf(((Number) value).doubleValue()));
                    } else if (value instanceof String) {
                        try {
                            user.setCurrentValue(new java.math.BigDecimal((String) value));
                        } catch (NumberFormatException e) {
                            // Ignore invalid number
                        }
                    }
                }
            }
            if (request.containsKey("averagePrice")) {
                Object priceValue = request.get("averagePrice");
                if (priceValue != null) {
                    if (priceValue instanceof Number) {
                        user.setAveragePrice(java.math.BigDecimal.valueOf(((Number) priceValue).doubleValue()));
                    } else if (priceValue instanceof String) {
                        try {
                            user.setAveragePrice(new java.math.BigDecimal((String) priceValue));
                        } catch (NumberFormatException e) {
                            // Ignore invalid number
                        }
                    }
                }
            }
            if (request.containsKey("entryDate")) {
                Object dateValue = request.get("entryDate");
                if (dateValue != null && dateValue instanceof String) {
                    try {
                        user.setEntryDate(java.time.LocalDate.parse((String) dateValue));
                    } catch (Exception e) {
                        // Ignore invalid date
                    }
                }
            }
            
            user = userRepository.save(user);
            
            // Build response DTO
            Map<String, Object> response = new HashMap<>();
            response.put("id", user.getId());
            response.put("email", user.getEmail());
            response.put("firstName", user.getFirstName());
            response.put("lastName", user.getLastName());
            response.put("avatarUrl", user.getAvatarUrl());
            response.put("coverImageUrl", user.getCoverImageUrl());
            response.put("gender", user.getGender());
            response.put("headline", user.getHeadline());
            response.put("company", user.getCompany());
            response.put("role", user.getRole());
            response.put("about", user.getAbout());
            response.put("location", user.getLocation());
            response.put("website", user.getWebsite());
            response.put("twitter", user.getTwitter());
            response.put("linkedin", user.getLinkedin());
            response.put("bodivasCompanyCode", user.getBodivasCompanyCode());
            response.put("bodivasCompanyName", user.getBodivasCompanyName());
            response.put("sharesOwned", user.getSharesOwned());
            response.put("sharePercentage", user.getSharePercentage());
            response.put("investmentAmount", user.getInvestmentAmount());
            response.put("currentValue", user.getCurrentValue());
            response.put("averagePrice", user.getAveragePrice());
            
            if (user.getEntryDate() != null) {
                response.put("entryDate", user.getEntryDate().toString());
            }
            
            if (user.getDateOfBirth() != null) {
                response.put("dateOfBirth", user.getDateOfBirth().toString());
            }
            if (user.getCreatedAt() != null) {
                response.put("createdAt", user.getCreatedAt().toString());
            }
            response.put("rule", user.getRule());
            
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse(e.getMessage()));
        }
    }
    
    @GetMapping("/admin/statistics")
    public ResponseEntity<?> getUserStatistics(Authentication authentication) {
        try {
            Long userId = getUserIdFromAuthentication(authentication);
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new ErrorResponse("Authentication required"));
            }
            
            if (!isAdmin(userId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(new ErrorResponse("Admin access required"));
            }
            
            // Calculate statistics
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime thirtyDaysAgo = now.minusDays(30);
            LocalDateTime sevenDaysAgo = now.minusDays(7);
            LocalDateTime startOfMonth = now.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
            LocalDateTime startOfWeek = now.minusDays(now.getDayOfWeek().getValue() - 1).withHour(0).withMinute(0).withSecond(0).withNano(0);
            
            long totalUsers = userRepository.count();
            long newUsersThisMonth = userRepository.countByCreatedAtAfter(startOfMonth);
            long newUsersThisWeek = userRepository.countByCreatedAtAfter(startOfWeek);
            long activeUsersLast30Days = userRepository.countByUpdatedAtAfter(thirtyDaysAgo);
            long activeUsersLast7Days = userRepository.countByUpdatedAtAfter(sevenDaysAgo);
            long adminUsers = userRepository.countByRule("ADMIN");
            long regularUsers = userRepository.countByRule("USER");
            
            Map<String, Object> statistics = new HashMap<>();
            statistics.put("totalUsers", totalUsers);
            statistics.put("newUsersThisMonth", newUsersThisMonth);
            statistics.put("newUsersThisWeek", newUsersThisWeek);
            statistics.put("activeUsersLast30Days", activeUsersLast30Days);
            statistics.put("activeUsersLast7Days", activeUsersLast7Days);
            statistics.put("adminUsers", adminUsers);
            statistics.put("regularUsers", regularUsers);
            
            return ResponseEntity.ok(statistics);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Error fetching user statistics: " + e.getMessage()));
        }
    }
    
    private Long getUserIdFromAuthentication(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        String email = userDetails.getUsername();
        
        // Get user ID from database
        return userRepository.findByEmail(email)
                .map(User::getId)
                .orElse(null);
    }
    
    private boolean isAdmin(Long userId) {
        return userRepository.findById(userId)
                .map(user -> "ADMIN".equals(user.getRule()))
                .orElse(false);
    }
    
    // Helper class
    @lombok.Data
    @lombok.AllArgsConstructor
    private static class ErrorResponse {
        private String message;
    }
}

