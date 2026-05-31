package com.stackcompany.stack.controller;

import com.stackcompany.stack.entity.Investment;
import com.stackcompany.stack.entity.User;
import com.stackcompany.stack.repository.InvestmentRepository;
import com.stackcompany.stack.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/investments")
@CrossOrigin(origins = "*")
public class InvestmentController {
    
    @Autowired
    private InvestmentRepository investmentRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @GetMapping
    public ResponseEntity<?> getUserInvestments(Authentication authentication) {
        try {
            Long userId = getUserIdFromAuthentication(authentication);
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new ErrorResponse("Authentication required"));
            }
            
            List<Investment> investments = investmentRepository.findByUserId(userId);
            List<Map<String, Object>> response = investments.stream()
                    .map(this::convertToResponse)
                    .collect(Collectors.toList());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse(e.getMessage()));
        }
    }
    
    @GetMapping("/user/{userId}")
    public ResponseEntity<?> getUserInvestments(@PathVariable Long userId) {
        try {
            List<Investment> investments = investmentRepository.findByUserId(userId);
            List<Map<String, Object>> response = investments.stream()
                    .map(this::convertToResponse)
                    .collect(Collectors.toList());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse(e.getMessage()));
        }
    }
    
    @PostMapping
    public ResponseEntity<?> createInvestment(
            @RequestBody Map<String, Object> request,
            Authentication authentication) {
        try {
            Long userId = getUserIdFromAuthentication(authentication);
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new ErrorResponse("Authentication required"));
            }
            
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            
            Investment investment = Investment.builder()
                    .user(user)
                    .bodivasCompanyCode((String) request.get("bodivasCompanyCode"))
                    .bodivasCompanyName((String) request.get("bodivasCompanyName"))
                    .sharesOwned(parseLong(request.get("sharesOwned")))
                    .sharePercentage(parseBigDecimal(request.get("sharePercentage")))
                    .investmentAmount(parseBigDecimal(request.get("investmentAmount")))
                    .averagePrice(parseBigDecimal(request.get("averagePrice")))
                    .currentValue(parseBigDecimal(request.get("currentValue")))
                    .entryDate(parseDate(request.get("entryDate")))
                    .build();
            
            investment = investmentRepository.save(investment);
            
            return ResponseEntity.status(HttpStatus.CREATED).body(convertToResponse(investment));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse(e.getMessage()));
        }
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<?> updateInvestment(
            @PathVariable Long id,
            @RequestBody Map<String, Object> request,
            Authentication authentication) {
        try {
            Long userId = getUserIdFromAuthentication(authentication);
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new ErrorResponse("Authentication required"));
            }
            
            Investment investment = investmentRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Investment not found"));
            
            if (!investment.getUser().getId().equals(userId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(new ErrorResponse("You can only update your own investments"));
            }
            
            if (request.containsKey("bodivasCompanyCode")) {
                investment.setBodivasCompanyCode((String) request.get("bodivasCompanyCode"));
            }
            if (request.containsKey("bodivasCompanyName")) {
                investment.setBodivasCompanyName((String) request.get("bodivasCompanyName"));
            }
            if (request.containsKey("sharesOwned")) {
                investment.setSharesOwned(parseLong(request.get("sharesOwned")));
            }
            if (request.containsKey("sharePercentage")) {
                investment.setSharePercentage(parseBigDecimal(request.get("sharePercentage")));
            }
            if (request.containsKey("investmentAmount")) {
                investment.setInvestmentAmount(parseBigDecimal(request.get("investmentAmount")));
            }
            if (request.containsKey("averagePrice")) {
                investment.setAveragePrice(parseBigDecimal(request.get("averagePrice")));
            }
            if (request.containsKey("currentValue")) {
                investment.setCurrentValue(parseBigDecimal(request.get("currentValue")));
            }
            if (request.containsKey("entryDate")) {
                investment.setEntryDate(parseDate(request.get("entryDate")));
            }
            
            investment = investmentRepository.save(investment);
            
            return ResponseEntity.ok(convertToResponse(investment));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse(e.getMessage()));
        }
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteInvestment(
            @PathVariable Long id,
            Authentication authentication) {
        try {
            Long userId = getUserIdFromAuthentication(authentication);
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new ErrorResponse("Authentication required"));
            }
            
            Investment investment = investmentRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Investment not found"));
            
            if (!investment.getUser().getId().equals(userId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(new ErrorResponse("You can only delete your own investments"));
            }
            
            investmentRepository.delete(investment);
            
            return ResponseEntity.ok(new MessageResponse("Investment deleted successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse(e.getMessage()));
        }
    }
    
    private Map<String, Object> convertToResponse(Investment investment) {
        Map<String, Object> response = new HashMap<>();
        response.put("id", investment.getId());
        response.put("bodivasCompanyCode", investment.getBodivasCompanyCode());
        response.put("bodivasCompanyName", investment.getBodivasCompanyName());
        response.put("sharesOwned", investment.getSharesOwned());
        response.put("sharePercentage", investment.getSharePercentage());
        response.put("investmentAmount", investment.getInvestmentAmount());
        response.put("averagePrice", investment.getAveragePrice());
        response.put("currentValue", investment.getCurrentValue());
        if (investment.getEntryDate() != null) {
            response.put("entryDate", investment.getEntryDate().toString());
        }
        return response;
    }
    
    private Long getUserIdFromAuthentication(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        String email = userDetails.getUsername();
        
        return userRepository.findByEmail(email)
                .map(User::getId)
                .orElse(null);
    }
    
    private Long parseLong(Object value) {
        if (value == null) return null;
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        if (value instanceof String) {
            try {
                return Long.parseLong((String) value);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }
    
    private BigDecimal parseBigDecimal(Object value) {
        if (value == null) return null;
        if (value instanceof Number) {
            return BigDecimal.valueOf(((Number) value).doubleValue());
        }
        if (value instanceof String) {
            try {
                return new BigDecimal((String) value);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }
    
    private LocalDate parseDate(Object value) {
        if (value == null) return null;
        if (value instanceof String) {
            try {
                return LocalDate.parse((String) value);
            } catch (Exception e) {
                return null;
            }
        }
        return null;
    }
    
    @lombok.Data
    @lombok.AllArgsConstructor
    private static class ErrorResponse {
        private String message;
    }
    
    @lombok.Data
    @lombok.AllArgsConstructor
    private static class MessageResponse {
        private String message;
    }
}

