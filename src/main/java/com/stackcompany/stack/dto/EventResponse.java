package com.stackcompany.stack.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventResponse {
    private Long id;
    private String title;
    private String description;
    private String location;
    private LocalDateTime eventDate;
    private LocalDateTime endDate;
    private String imageUrl;
    private String category;
    private Integer maxAttendees;
    private Integer currentAttendees;
    private Integer availableSeats;
    private BigDecimal price;
    private Boolean isOnline;
    private String onlineLink;
    private String status;
    private Boolean isPublished;
    private Boolean isEnrolled;
    private String userTicketCode;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private AuthorInfo author;
    
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

