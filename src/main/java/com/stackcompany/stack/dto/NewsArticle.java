package com.stackcompany.stack.dto;

import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class NewsArticle {
    private String title;
    private String body;
    private String imageUrl;
    private String sourceUrl;
    private String sourceName;
}
