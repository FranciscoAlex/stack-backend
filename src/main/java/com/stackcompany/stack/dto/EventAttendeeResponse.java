package com.stackcompany.stack.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventAttendeeResponse {
    private Long id;
    private Long eventId;
    private String eventTitle;
    private LocalDateTime eventDate;
    private String eventLocation;
    private Boolean isOnline;
    private String onlineLink;
    private String ticketCode;
    private String status;
    private LocalDateTime checkedInAt;
    private LocalDateTime createdAt;
    private AttendeeInfo attendee;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AttendeeInfo {
        private Long id;
        private String email;
        private String firstName;
        private String lastName;
        private String avatar;
    }
}

