package com.stackcompany.stack.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class CrowdfundingCompanyRequest {
    @NotBlank(message = "Title is required")
    @Size(max = 255, message = "Title must not exceed 255 characters")
    private String title;
    
    @Size(max = 500, message = "Subtitle must not exceed 500 characters")
    private String subtitle;
    
    @NotBlank(message = "Description is required")
    private String description;
    
    @NotNull(message = "Goal amount is required")
    private BigDecimal goalAmount;
    
    @Size(max = 500, message = "Icon URL must not exceed 500 characters")
    private String iconUrl;
    
    @Size(max = 5, message = "Maximum 5 images allowed")
    private String[] imageUrls;
    
    @Size(max = 500, message = "Product URL must not exceed 500 characters")
    private String productUrl;
    
    private LocalDate startDate;
    
    @NotNull(message = "End date is required")
    private LocalDate endDate;
    
    private String[] tags;
}

