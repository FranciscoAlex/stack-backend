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
public class CommentResponse {
    private Long id;
    private Long postId;
    private Long parentId;
    private String content;
    private Integer votes;
    private Integer repliesCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private AuthorInfo author;
    private String userVote; // 'up', 'down', or null
    private List<CommentResponse> replies; // Nested replies
    
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

