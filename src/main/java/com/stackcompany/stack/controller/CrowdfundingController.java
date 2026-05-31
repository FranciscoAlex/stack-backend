package com.stackcompany.stack.controller;

import com.stackcompany.stack.dto.CrowdfundingCompanyRequest;
import com.stackcompany.stack.dto.CrowdfundingCompanyResponse;
import com.stackcompany.stack.entity.User;
import com.stackcompany.stack.repository.UserRepository;
import com.stackcompany.stack.service.CrowdfundingService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/crowdfunding")
@CrossOrigin(origins = "*")
public class CrowdfundingController {
    
    @Autowired
    private CrowdfundingService crowdfundingService;
    
    @Autowired
    private UserRepository userRepository;
    
    @PostMapping
    public ResponseEntity<?> createCompany(@Valid @RequestBody CrowdfundingCompanyRequest request, Authentication authentication) {
        try {
            Long userId = getUserIdFromAuthentication(authentication);
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new ErrorResponse("Authentication required"));
            }
            // Validate endDate is not null
            if (request.getEndDate() == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(new ErrorResponse("End date is required"));
            }
            CrowdfundingCompanyResponse response = crowdfundingService.createCompany(userId, request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse(e.getMessage()));
        }
    }
    
    @ExceptionHandler(org.springframework.http.converter.HttpMessageNotReadableException.class)
    public ResponseEntity<?> handleHttpMessageNotReadableException(org.springframework.http.converter.HttpMessageNotReadableException e) {
        String message = e.getMessage();
        if (message != null && message.contains("LocalDate")) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse("Invalid date format. Please use YYYY-MM-DD format (e.g., 2024-12-31)"));
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse("Invalid request format: " + (message != null ? message : "Unknown error")));
    }
    
    @GetMapping
    public ResponseEntity<Page<CrowdfundingCompanyResponse>> getAllCompanies(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {
        Long userId = getUserIdFromAuthentication(authentication);
        Pageable pageable = PageRequest.of(page, size);
        Page<CrowdfundingCompanyResponse> companies = crowdfundingService.getAllCompanies(pageable, userId);
        return ResponseEntity.ok(companies);
    }
    
    @GetMapping("/top")
    public ResponseEntity<Page<CrowdfundingCompanyResponse>> getTopCompanies(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {
        Long userId = getUserIdFromAuthentication(authentication);
        Pageable pageable = PageRequest.of(page, size);
        Page<CrowdfundingCompanyResponse> companies = crowdfundingService.getTopCompanies(pageable, userId);
        return ResponseEntity.ok(companies);
    }
    
    @GetMapping("/top3")
    public ResponseEntity<List<CrowdfundingCompanyResponse>> getTop3Companies(Authentication authentication) {
        Long userId = getUserIdFromAuthentication(authentication);
        List<CrowdfundingCompanyResponse> companies = crowdfundingService.getTop3Companies(userId);
        return ResponseEntity.ok(companies);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<?> getCompanyById(@PathVariable Long id, Authentication authentication) {
        try {
            Long userId = getUserIdFromAuthentication(authentication);
            CrowdfundingCompanyResponse response = crowdfundingService.getCompanyById(id, userId);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponse(e.getMessage()));
        }
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<?> updateCompany(@PathVariable Long id, 
                                          @Valid @RequestBody CrowdfundingCompanyRequest request,
                                          Authentication authentication) {
        try {
            Long userId = getUserIdFromAuthentication(authentication);
            CrowdfundingCompanyResponse response = crowdfundingService.updateCompany(id, userId, request);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse(e.getMessage()));
        }
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteCompany(@PathVariable Long id, Authentication authentication) {
        try {
            Long userId = getUserIdFromAuthentication(authentication);
            crowdfundingService.deleteCompany(id, userId);
            return ResponseEntity.ok(new MessageResponse("Company deleted successfully"));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse(e.getMessage()));
        }
    }
    
    @PostMapping("/{id}/vote")
    public ResponseEntity<?> voteCompany(@PathVariable Long id, Authentication authentication) {
        try {
            Long userId = getUserIdFromAuthentication(authentication);
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new ErrorResponse("Authentication required"));
            }
            crowdfundingService.voteCompany(id, userId);
            return ResponseEntity.ok().build();
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse(e.getMessage()));
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
    
    // Helper classes
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

