package com.stackcompany.stack.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "investments")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Investment {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @Column(name = "bodivas_company_code", length = 10, nullable = false)
    private String bodivasCompanyCode;
    
    @Column(name = "bodivas_company_name", length = 255, nullable = false)
    private String bodivasCompanyName;
    
    @Column(name = "shares_owned", nullable = false)
    private Long sharesOwned;
    
    @Column(name = "share_percentage", precision = 5, scale = 2)
    private BigDecimal sharePercentage;
    
    @Column(name = "investment_amount", precision = 15, scale = 2, nullable = false)
    private BigDecimal investmentAmount;
    
    @Column(name = "entry_date")
    private LocalDate entryDate;
    
    @Column(name = "current_value", precision = 15, scale = 2)
    private BigDecimal currentValue;
    
    @Column(name = "average_price", precision = 10, scale = 2, nullable = false)
    private BigDecimal averagePrice;
    
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}

