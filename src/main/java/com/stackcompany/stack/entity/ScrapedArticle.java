package com.stackcompany.stack.entity;

import jakarta.persistence.*;
import lombok.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;

@Entity
@Table(name = "scraped_articles")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class ScrapedArticle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "source_url", length = 1000)
    private String sourceUrl;

    @Column(name = "url_hash", length = 64, unique = true)
    private String urlHash;

    @Column(columnDefinition = "TEXT")
    private String title;

    @Column(name = "title_hash", length = 64)
    private String titleHash;

    @Column(name = "source_name", length = 100)
    private String sourceName;

    @Column(length = 20)
    private String status; // POSTED, DUPLICATE, FAILED

    @Column(name = "post_id")
    private Long postId;

    @Column(name = "scraped_at")
    private LocalDateTime scrapedAt;

    @Column(name = "posted_at")
    private LocalDateTime postedAt;

    public static String generateHash(String input) {
        if (input == null) return null;
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) sb.append(String.format("%02x", b));
            return sb.toString().substring(0, 32);
        } catch (Exception e) {
            return Integer.toHexString(input.hashCode());
        }
    }
}
