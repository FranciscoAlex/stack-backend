package com.stackcompany.stack.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "crowdfunding_companies")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CrowdfundingCompany {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @Column(nullable = false, length = 255)
    private String title;
    
    @Column(length = 500)
    private String subtitle;
    
    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;
    
    @Column(name = "goal_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal goalAmount;
    
    @Column(name = "raised_amount", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal raisedAmount = BigDecimal.ZERO;
    
    @Column(name = "icon_url", length = 500)
    private String iconUrl;
    
    @Column(name = "image_urls", columnDefinition = "TEXT[]")
    private String[] imageUrls;
    
    @Column(name = "product_url", length = 500)
    private String productUrl;
    
    @Column(name = "start_date")
    private LocalDate startDate;
    
    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;
    
    @Column(nullable = false)
    @Builder.Default
    private Integer votes = 0;
    
    @Column(name = "comments_count", nullable = false)
    @Builder.Default
    private Integer commentsCount = 0;
    
    @Column(columnDefinition = "TEXT[]")
    private String[] tags;
    
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

