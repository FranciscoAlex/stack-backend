package com.stackcompany.stack.controller;

import com.stackcompany.stack.dto.BannerConfigRequest;
import com.stackcompany.stack.entity.BannerConfig;
import com.stackcompany.stack.entity.User;
import com.stackcompany.stack.repository.BannerConfigRepository;
import com.stackcompany.stack.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/banner")
@CrossOrigin(origins = "*")
public class BannerConfigController {
    
    @Autowired
    private BannerConfigRepository bannerConfigRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    /**
     * Get banner config for a specific page (public endpoint)
     */
    @GetMapping("/{pageKey}")
    public ResponseEntity<?> getBannerByPageKey(@PathVariable String pageKey) {
        return bannerConfigRepository.findByPageKey(pageKey)
                .filter(BannerConfig::isEnabled)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    
    /**
     * Get all banner configs (admin only)
     */
    @GetMapping("/admin/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<BannerConfig>> getAllBanners(Authentication authentication) {
        if (!isAdmin(authentication)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(bannerConfigRepository.findAll());
    }
    
    /**
     * Create or update banner config (admin only)
     */
    @PostMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> createOrUpdateBanner(
            @RequestBody BannerConfigRequest request,
            Authentication authentication) {
        
        if (!isAdmin(authentication)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ErrorResponse("Unauthorized: Admin access required"));
        }
        
        if (request.getPageKey() == null || request.getPageKey().trim().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(new ErrorResponse("Page key is required"));
        }
        
        BannerConfig banner = bannerConfigRepository.findByPageKey(request.getPageKey())
                .orElse(BannerConfig.builder()
                        .pageKey(request.getPageKey())
                        .build());
        
        banner.setImageUrl(request.getImageUrl());
        banner.setTitle(request.getTitle());
        banner.setSubtitle(request.getSubtitle());
        banner.setLinkUrl(request.getLinkUrl());
        banner.setEnabled(request.getEnabled() != null ? request.getEnabled() : true);
        
        BannerConfig saved = bannerConfigRepository.save(banner);
        return ResponseEntity.ok(saved);
    }
    
    /**
     * Delete banner config (admin only)
     */
    @DeleteMapping("/admin/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteBanner(@PathVariable Long id, Authentication authentication) {
        if (!isAdmin(authentication)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ErrorResponse("Unauthorized: Admin access required"));
        }
        
        if (!bannerConfigRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        
        bannerConfigRepository.deleteById(id);
        return ResponseEntity.ok(new MessageResponse("Banner deleted successfully"));
    }
    
    private boolean isAdmin(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        String email = userDetails.getUsername();
        
        return userRepository.findByEmail(email)
                .map(user -> "ADMIN".equals(user.getRule()))
                .orElse(false);
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

