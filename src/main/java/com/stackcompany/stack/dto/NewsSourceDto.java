package com.stackcompany.stack.dto;

import lombok.*;
import java.util.List;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class NewsSourceDto {
    private Long id;
    private String name;
    private String baseUrl;
    private String listUrl;
    private boolean enabled;
    private int articlesPerRun;
    private String selectorsJson;
    private boolean legacy;
    private String status;
    private String lastScrapedAt;
    private String lastError;
    private List<String> previewTitles;
    private String analyzeMessage;
}
