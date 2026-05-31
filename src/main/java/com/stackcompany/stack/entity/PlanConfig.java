package com.stackcompany.stack.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "plan_config")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlanConfig {

    @Id
    @Column(name = "plan_id", length = 20)
    private String planId;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "stripe_price_id", length = 200)
    private String stripePriceId;

    @Column(name = "display_price")
    private Double displayPrice;

    @Column(name = "original_price")
    private Double originalPrice;

    @Column(length = 50)
    private String discount;

    @Column(length = 50)
    private String period;

    @Builder.Default
    private boolean popular = false;

    @Column(name = "exchange_rate")
    @Builder.Default
    private Double exchangeRate = 950.0;

    @ElementCollection
    @CollectionTable(name = "plan_features", joinColumns = @JoinColumn(name = "plan_id"))
    @Column(name = "feature", columnDefinition = "TEXT")
    private List<String> features;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    protected void onUpdate() { updatedAt = LocalDateTime.now(); }
}
