package com.stackcompany.stack.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "news_sources")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class NewsSource {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(name = "base_url", nullable = false, length = 500)
    private String baseUrl;

    @Column(name = "list_url", nullable = false, length = 500)
    private String listUrl;

    @Builder.Default
    private boolean enabled = true;

    @Column(name = "articles_per_run")
    @Builder.Default
    private int articlesPerRun = 2;

    @Column(name = "selectors_json", columnDefinition = "TEXT")
    private String selectorsJson;

    @Column(name = "is_legacy")
    @Builder.Default
    private boolean legacy = false;

    @Column(length = 30)
    @Builder.Default
    private String status = "NEEDS_CONFIG";

    @Column(name = "last_scraped_at")
    private LocalDateTime lastScrapedAt;

    @Column(name = "last_error", columnDefinition = "TEXT")
    private String lastError;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist  protected void onCreate()  { createdAt = updatedAt = LocalDateTime.now(); }
    @PreUpdate   protected void onUpdate()  { updatedAt = LocalDateTime.now(); }
}
