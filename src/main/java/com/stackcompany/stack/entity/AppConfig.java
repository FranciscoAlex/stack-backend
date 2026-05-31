package com.stackcompany.stack.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "app_config")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class AppConfig {

    @Id
    @Column(name = "config_key", length = 100)
    private String configKey;

    @Column(name = "config_value", columnDefinition = "TEXT")
    private String configValue;

    @Column(length = 500)
    private String description;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist @PreUpdate
    protected void onUpdate() { updatedAt = LocalDateTime.now(); }
}
