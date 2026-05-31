package com.stackcompany.stack.controller;

import com.stackcompany.stack.dto.SlideshowConfigRequest;
import com.stackcompany.stack.entity.SlideshowConfig;
import com.stackcompany.stack.repository.SlideshowConfigRepository;
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
@RequestMapping("/api/slideshow")
@CrossOrigin(origins = "*")
public class SlideshowConfigController {
    
    @Autowired
    private SlideshowConfigRepository slideshowConfigRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    /**
     * Get slideshow images for a specific page (public endpoint)
     */
    @GetMapping("/{pageKey}")
    public ResponseEntity<List<SlideshowConfig>> getSlideshowByPageKey(@PathVariable String pageKey) {
        List<SlideshowConfig> slides = slideshowConfigRepository
                .findByPageKeyAndEnabledTrueOrderByDisplayOrderAsc(pageKey);
        return ResponseEntity.ok(slides);
    }
    
    /**
     * Get all slideshow configs for a page (admin only - includes disabled)
     */
    @GetMapping("/admin/{pageKey}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getAllSlideshowByPageKey(
            @PathVariable String pageKey,
            Authentication authentication) {
        
        if (!isAdmin(authentication)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        List<SlideshowConfig> slides = slideshowConfigRepository
                .findByPageKeyOrderByDisplayOrderAsc(pageKey);
        return ResponseEntity.ok(slides);
    }
    
    /**
     * Create slideshow config (admin only)
     */
    @PostMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> createSlideshow(
            @RequestBody SlideshowConfigRequest request,
            Authentication authentication) {
        
        if (!isAdmin(authentication)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ErrorResponse("Unauthorized: Admin access required"));
        }
        
        if (request.getPageKey() == null || request.getPageKey().trim().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(new ErrorResponse("Page key is required"));
        }
        
        if (request.getImageUrl() == null || request.getImageUrl().trim().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(new ErrorResponse("Image URL is required"));
        }
        
        SlideshowConfig slide = SlideshowConfig.builder()
                .pageKey(request.getPageKey())
                .imageUrl(request.getImageUrl())
                .displayOrder(request.getDisplayOrder())
                .title(request.getTitle())
                .subtitle(request.getSubtitle())
                .enabled(request.getEnabled() != null ? request.getEnabled() : true)
                .build();
        
        SlideshowConfig saved = slideshowConfigRepository.save(slide);
        return ResponseEntity.ok(saved);
    }
    
    /**
     * Update slideshow config (admin only)
     */
    @PutMapping("/admin/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateSlideshow(
            @PathVariable Long id,
            @RequestBody SlideshowConfigRequest request,
            Authentication authentication) {
        
        if (!isAdmin(authentication)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ErrorResponse("Unauthorized: Admin access required"));
        }
        
        SlideshowConfig slide = slideshowConfigRepository.findById(id)
                .orElse(null);
        
        if (slide == null) {
            return ResponseEntity.notFound().build();
        }
        
        if (request.getImageUrl() != null) {
            slide.setImageUrl(request.getImageUrl());
        }
        if (request.getDisplayOrder() != null) {
            slide.setDisplayOrder(request.getDisplayOrder());
        }
        if (request.getTitle() != null) {
            slide.setTitle(request.getTitle());
        }
        if (request.getSubtitle() != null) {
            slide.setSubtitle(request.getSubtitle());
        }
        if (request.getEnabled() != null) {
            slide.setEnabled(request.getEnabled());
        }
        
        SlideshowConfig saved = slideshowConfigRepository.save(slide);
        return ResponseEntity.ok(saved);
    }
    
    /**
     * Delete slideshow config (admin only)
     */
    @DeleteMapping("/admin/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteSlideshow(@PathVariable Long id, Authentication authentication) {
        if (!isAdmin(authentication)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ErrorResponse("Unauthorized: Admin access required"));
        }
        
        if (!slideshowConfigRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        
        slideshowConfigRepository.deleteById(id);
        return ResponseEntity.ok(new MessageResponse("Slideshow deleted successfully"));
    }
    
    /**
     * Batch update slideshows for a page (admin only)
     */
    @PostMapping("/admin/batch/{pageKey}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> batchUpdateSlideshows(
            @PathVariable String pageKey,
            @RequestBody List<SlideshowConfigRequest> requests,
            Authentication authentication) {
        
        if (!isAdmin(authentication)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ErrorResponse("Unauthorized: Admin access required"));
        }
        
        // Delete all existing slides for this page
        List<SlideshowConfig> existingSlides = slideshowConfigRepository
                .findByPageKeyOrderByDisplayOrderAsc(pageKey);
        slideshowConfigRepository.deleteAll(existingSlides);
        
        // Create new slides
        int order = 0;
        for (SlideshowConfigRequest request : requests) {
            if (request.getImageUrl() != null && !request.getImageUrl().trim().isEmpty()) {
                SlideshowConfig slide = SlideshowConfig.builder()
                        .pageKey(pageKey)
                        .imageUrl(request.getImageUrl())
                        .displayOrder(order++)
                        .title(request.getTitle())
                        .subtitle(request.getSubtitle())
                        .enabled(request.getEnabled() != null ? request.getEnabled() : true)
                        .build();
                slideshowConfigRepository.save(slide);
            }
        }
        
        List<SlideshowConfig> updatedSlides = slideshowConfigRepository
                .findByPageKeyOrderByDisplayOrderAsc(pageKey);
        return ResponseEntity.ok(updatedSlides);
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

