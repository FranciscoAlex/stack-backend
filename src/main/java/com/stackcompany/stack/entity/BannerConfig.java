package com.stackcompany.stack.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "banner_config")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BannerConfig {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "page_key", unique = true, nullable = false, length = 50)
    private String pageKey; // 'home', 'crowdfunding', 'formation', etc.
    
    @Column(name = "image_url", length = 1000)
    private String imageUrl;
    
    @Column(name = "title", length = 200)
    private String title;
    
    @Column(name = "subtitle", length = 500)
    private String subtitle;
    
    @Column(name = "link_url", length = 1000)
    private String linkUrl;
    
    @Column(name = "enabled", columnDefinition = "BOOLEAN DEFAULT TRUE")
    @Builder.Default
    private boolean enabled = true;
    
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}

