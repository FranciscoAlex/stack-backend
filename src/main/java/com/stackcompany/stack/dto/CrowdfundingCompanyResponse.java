package com.stackcompany.stack.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CrowdfundingCompanyResponse {
    private Long id;
    private String title;
    private String subtitle;
    private String description;
    private BigDecimal goalAmount;
    private BigDecimal raisedAmount;
    private String iconUrl;
    private String[] imageUrls;
    private String productUrl;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer votes;
    private Integer commentsCount;
    private String[] tags;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private AuthorInfo author;
    private Boolean hasVoted; // true if current user has voted for this company
    
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

