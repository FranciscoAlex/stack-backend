package com.stackcompany.stack.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class EventRequest {
    @NotBlank(message = "Title is required")
    @Size(max = 255, message = "Title must not exceed 255 characters")
    private String title;
    
    @Size(max = 10000, message = "Description must not exceed 10000 characters")
    private String description;
    
    @Size(max = 500, message = "Location must not exceed 500 characters")
    private String location;
    
    @NotNull(message = "Event date is required")
    private LocalDateTime eventDate;
    
    private LocalDateTime endDate;
    
    @Size(max = 500, message = "Image URL must not exceed 500 characters")
    private String imageUrl;
    
    @Size(max = 100, message = "Category must not exceed 100 characters")
    private String category;
    
    private Integer maxAttendees;
    
    private BigDecimal price;
    
    private Boolean isOnline;
    
    @Size(max = 500, message = "Online link must not exceed 500 characters")
    private String onlineLink;
}

