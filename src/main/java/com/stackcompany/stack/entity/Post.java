package com.stackcompany.stack.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "posts")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Post {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "image_url", columnDefinition = "TEXT")
    private String imageUrlsJson; // Stored as JSON array string (reusing image_url column for backward
                                  // compatibility)

    // Transient field for easier access
    @Transient
    private List<String> imageUrls;

    // Helper methods to convert between JSON string and List
    public List<String> getImageUrls() {
        if (imageUrls == null && imageUrlsJson != null && !imageUrlsJson.trim().isEmpty()) {
            try {
                ObjectMapper mapper = new ObjectMapper();
                imageUrls = mapper.readValue(imageUrlsJson, new TypeReference<List<String>>() {
                });
            } catch (Exception e) {
                // Fallback: try to parse as comma-separated (for backward compatibility)
                if (imageUrlsJson.contains("[") || imageUrlsJson.contains("{")) {
                    imageUrls = new ArrayList<>();
                } else {
                    // Legacy single image URL or comma-separated
                    imageUrls = new ArrayList<>();
                    String[] urls = imageUrlsJson.split(",");
                    for (String url : urls) {
                        String trimmed = url.trim();
                        if (!trimmed.isEmpty()) {
                            imageUrls.add(trimmed);
                        }
                    }
                }
            }
        }
        return imageUrls != null ? imageUrls : new ArrayList<>();
    }

    public void setImageUrls(List<String> imageUrls) {
        this.imageUrls = imageUrls;
        if (imageUrls == null || imageUrls.isEmpty()) {
            this.imageUrlsJson = null;
        } else {
            try {
                ObjectMapper mapper = new ObjectMapper();
                this.imageUrlsJson = mapper.writeValueAsString(imageUrls);
            } catch (Exception e) {
                // Fallback to comma-separated
                this.imageUrlsJson = String.join(",", imageUrls);
            }
        }
    }

    // Backward compatibility: get first image URL
    public String getImageUrl() {
        List<String> urls = getImageUrls();
        return urls.isEmpty() ? null : urls.get(0);
    }

    // Backward compatibility: set single image URL
    public void setImageUrl(String imageUrl) {
        if (imageUrl == null || imageUrl.trim().isEmpty()) {
            setImageUrls(new ArrayList<>());
        } else {
            List<String> urls = new ArrayList<>();
            urls.add(imageUrl);
            setImageUrls(urls);
        }
    }

    @Column(nullable = false)
    @Builder.Default
    private Integer votes = 0;

    @Column(name = "comments_count", nullable = false)
    @Builder.Default
    private Integer commentsCount = 0;

    @Column(name = "shares_count", nullable = false)
    @Builder.Default
    private Integer sharesCount = 0;

    @Column(nullable = false)
    @Builder.Default
    private Integer views = 0;

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String status = "PENDING"; // 'PENDING', 'APPROVED', 'REJECTED'

    @Column(nullable = false)
    @Builder.Default
    private Boolean pinned = false; // Pinned posts appear at the top

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        // Ensure imageUrlsJson is synced before persist
        if (imageUrls != null) {
            setImageUrls(imageUrls);
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
        // Ensure imageUrlsJson is synced before update
        if (imageUrls != null) {
            setImageUrls(imageUrls);
        }
    }

    @Builder.Default
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "post_tags", joinColumns = @JoinColumn(name = "post_id"), inverseJoinColumns = @JoinColumn(name = "tag_id"))
    private List<Tag> tags = new ArrayList<>();

    @PostLoad
    protected void onLoad() {
        // Load imageUrls from JSON when entity is loaded
        // Reset transient field to force re-parsing
        this.imageUrls = null;
        if (imageUrlsJson != null && !imageUrlsJson.trim().isEmpty()) {
            getImageUrls(); // This will parse the JSON
        }
    }
}
