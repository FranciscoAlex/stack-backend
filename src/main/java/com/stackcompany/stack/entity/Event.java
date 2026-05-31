package com.stackcompany.stack.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "events")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Event {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @Column(nullable = false, length = 255)
    private String title;
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    @Column(length = 500)
    private String location;
    
    @Column(name = "event_date", nullable = false)
    private LocalDateTime eventDate;
    
    @Column(name = "end_date")
    private LocalDateTime endDate;
    
    @Column(name = "image_url", length = 500)
    private String imageUrl;
    
    @Column(length = 100)
    private String category;
    
    @Column(name = "max_attendees")
    private Integer maxAttendees;
    
    @Column(name = "current_attendees", nullable = false)
    @Builder.Default
    private Integer currentAttendees = 0;
    
    @Column(precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal price = BigDecimal.ZERO;
    
    @Column(name = "is_online", nullable = false)
    @Builder.Default
    private Boolean isOnline = false;
    
    @Column(name = "online_link", length = 500)
    private String onlineLink;
    
    @Column(nullable = false, length = 20)
    @Builder.Default
    private String status = "PENDING"; // 'PENDING', 'APPROVED', 'REJECTED', 'CANCELLED'
    
    @Column(name = "is_published", nullable = false)
    @Builder.Default
    private Boolean isPublished = false;
    
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

