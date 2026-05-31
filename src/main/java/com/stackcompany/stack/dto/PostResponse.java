package com.stackcompany.stack.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PostResponse {
    private Long id;
    private String content;
    private String imageUrl; // Deprecated, use imageUrls instead (kept for backward compatibility)
    private List<String> imageUrls; // List of image URLs (up to 5)
    private List<String> tags;
    private Integer votes;
    private Integer commentsCount;
    private Integer sharesCount;
    private Integer views;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private AuthorInfo author;
    private String userVote; // 'up', 'down', or null
    private Boolean isBookmarked; // true if current user has bookmarked this post
    private Boolean pinned; // true if post is pinned

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AuthorInfo {
        private Long id;
        private String email;
        private String firstName;
        private String lastName;
        private String avatar;
    }
}
