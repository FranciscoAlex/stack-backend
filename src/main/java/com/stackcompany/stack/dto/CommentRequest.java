package com.stackcompany.stack.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommentRequest {
    private String content;
    private Long parentId; // null for top-level comments, ID of parent comment for replies
}

