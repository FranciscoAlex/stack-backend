package com.stackcompany.stack.controller;

import com.stackcompany.stack.dto.EventAttendeeResponse;
import com.stackcompany.stack.dto.EventRequest;
import com.stackcompany.stack.dto.EventResponse;
import com.stackcompany.stack.entity.User;
import com.stackcompany.stack.repository.UserRepository;
import com.stackcompany.stack.service.EventService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/events")
@CrossOrigin(origins = "*")
public class EventController {
    
    @Autowired
    private EventService eventService;
    
    @Autowired
    private UserRepository userRepository;
    
    /**
     * Publish an event - Most important endpoint for publishing events
     */
    @PostMapping("/publish")
    public ResponseEntity<?> publishEvent(@Valid @RequestBody EventRequest request, Authentication authentication) {
        try {
            Long userId = getUserIdFromAuthentication(authentication);
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new ErrorResponse("Authentication required"));
            }
            EventResponse response = eventService.publishEvent(userId, request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse(e.getMessage()));
        }
    }
    
    /**
     * Create an event as draft (not published)
     */
    @PostMapping
    public ResponseEntity<?> createEvent(@Valid @RequestBody EventRequest request, Authentication authentication) {
        try {
            Long userId = getUserIdFromAuthentication(authentication);
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new ErrorResponse("Authentication required"));
            }
            EventResponse response = eventService.createEvent(userId, request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse(e.getMessage()));
        }
    }
    
    /**
     * Publish an existing event (change from draft to published)
     */
    @PostMapping("/{id}/publish")
    public ResponseEntity<?> publishExistingEvent(@PathVariable Long id, Authentication authentication) {
        try {
            Long userId = getUserIdFromAuthentication(authentication);
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new ErrorResponse("Authentication required"));
            }
            EventResponse response = eventService.publishExistingEvent(id, userId);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse(e.getMessage()));
        }
    }
    
    /**
     * Unpublish an event (change from published to draft)
     */
    @PostMapping("/{id}/unpublish")
    public ResponseEntity<?> unpublishEvent(@PathVariable Long id, Authentication authentication) {
        try {
            Long userId = getUserIdFromAuthentication(authentication);
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new ErrorResponse("Authentication required"));
            }
            EventResponse response = eventService.unpublishEvent(id, userId);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse(e.getMessage()));
        }
    }
    
    /**
     * Get all published upcoming events
     */
    @GetMapping("/upcoming")
    public ResponseEntity<Page<EventResponse>> getPublishedUpcomingEvents(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<EventResponse> events = eventService.getPublishedUpcomingEvents(pageable);
        return ResponseEntity.ok(events);
    }
    
    /**
     * Get all published events (including past)
     */
    @GetMapping
    public ResponseEntity<Page<EventResponse>> getPublishedEvents(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<EventResponse> events = eventService.getPublishedEvents(pageable);
        return ResponseEntity.ok(events);
    }
    
    /**
     * Get event by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getEventById(@PathVariable Long id, Authentication authentication) {
        try {
            Long userId = getUserIdFromAuthentication(authentication);
            EventResponse response = eventService.getEventById(id, userId);
            
            // Check if user can view this event
            // Only published and approved events are visible to non-owners
            if (userId == null || !response.getAuthor().getId().equals(userId)) {
                if (!response.getIsPublished() || !"APPROVED".equals(response.getStatus())) {
                    return ResponseEntity.status(HttpStatus.NOT_FOUND)
                            .body(new ErrorResponse("Event not found"));
                }
            }
            
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponse(e.getMessage()));
        }
    }
    
    /**
     * Get events by user
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<?> getUserEvents(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {
        try {
            Long currentUserId = getUserIdFromAuthentication(authentication);
            Pageable pageable = PageRequest.of(page, size);
            Page<EventResponse> events = eventService.getUserEvents(userId, pageable);
            
            // If not the owner, filter to only show published and approved events
            if (currentUserId == null || !currentUserId.equals(userId)) {
                // Filter in memory (could be optimized with query)
                List<EventResponse> filteredContent = events.getContent().stream()
                        .filter(event -> event.getIsPublished() && "APPROVED".equals(event.getStatus()))
                        .collect(java.util.stream.Collectors.toList());
                
                Page<EventResponse> filteredPage = new PageImpl<>(
                        filteredContent, pageable, filteredContent.size());
                return ResponseEntity.ok(filteredPage);
            }
            
            return ResponseEntity.ok(events);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponse(e.getMessage()));
        }
    }
    
    /**
     * Update an event
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> updateEvent(@PathVariable Long id,
                                       @Valid @RequestBody EventRequest request,
                                       Authentication authentication) {
        try {
            Long userId = getUserIdFromAuthentication(authentication);
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new ErrorResponse("Authentication required"));
            }
            EventResponse response = eventService.updateEvent(id, userId, request);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse(e.getMessage()));
        }
    }
    
    /**
     * Delete an event
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteEvent(@PathVariable Long id, Authentication authentication) {
        try {
            Long userId = getUserIdFromAuthentication(authentication);
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new ErrorResponse("Authentication required"));
            }
            eventService.deleteEvent(id, userId);
            return ResponseEntity.ok(new MessageResponse("Event deleted successfully"));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse(e.getMessage()));
        }
    }
    
    /**
     * Admin endpoints
     */
    @GetMapping("/admin/pending")
    public ResponseEntity<?> getPendingEvents(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {
        try {
            Long userId = getUserIdFromAuthentication(authentication);
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new ErrorResponse("Authentication required"));
            }
            
            if (!isAdmin(userId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(new ErrorResponse("Admin access required"));
            }
            
            Pageable pageable = PageRequest.of(page, size);
            Page<EventResponse> events = eventService.getPendingEvents(pageable);
            return ResponseEntity.ok(events);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse(e.getMessage()));
        }
    }
    
    @PostMapping("/{id}/approve")
    public ResponseEntity<?> approveEvent(@PathVariable Long id, Authentication authentication) {
        try {
            Long userId = getUserIdFromAuthentication(authentication);
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new ErrorResponse("Authentication required"));
            }
            
            if (!isAdmin(userId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(new ErrorResponse("Admin access required"));
            }
            
            EventResponse response = eventService.approveEvent(id);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse(e.getMessage()));
        }
    }
    
    @PostMapping("/{id}/reject")
    public ResponseEntity<?> rejectEvent(@PathVariable Long id, Authentication authentication) {
        try {
            Long userId = getUserIdFromAuthentication(authentication);
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new ErrorResponse("Authentication required"));
            }
            
            if (!isAdmin(userId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(new ErrorResponse("Admin access required"));
            }
            
            EventResponse response = eventService.rejectEvent(id);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse(e.getMessage()));
        }
    }
    
    /**
     * Enroll in an event
     */
    @PostMapping("/{id}/enroll")
    public ResponseEntity<?> enrollInEvent(@PathVariable Long id, Authentication authentication) {
        try {
            Long userId = getUserIdFromAuthentication(authentication);
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new ErrorResponse("É necessário fazer login para se inscrever"));
            }
            
            EventAttendeeResponse response = eventService.enrollInEvent(id, userId);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse(e.getMessage()));
        }
    }
    
    /**
     * Cancel enrollment
     */
    @DeleteMapping("/{id}/enroll")
    public ResponseEntity<?> cancelEnrollment(@PathVariable Long id, Authentication authentication) {
        try {
            Long userId = getUserIdFromAuthentication(authentication);
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new ErrorResponse("Authentication required"));
            }
            
            eventService.cancelEnrollment(id, userId);
            return ResponseEntity.ok(new MessageResponse("Inscrição cancelada com sucesso"));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse(e.getMessage()));
        }
    }
    
    /**
     * Check if user is enrolled in an event
     */
    @GetMapping("/{id}/enrollment")
    public ResponseEntity<?> checkEnrollment(@PathVariable Long id, Authentication authentication) {
        try {
            Long userId = getUserIdFromAuthentication(authentication);
            if (userId == null) {
                return ResponseEntity.ok(Map.of("isEnrolled", false, "enrollment", (Object) null));
            }
            
            boolean isEnrolled = eventService.isUserEnrolled(id, userId);
            EventAttendeeResponse enrollment = eventService.getUserEnrollment(id, userId);
            
            return ResponseEntity.ok(Map.of(
                "isEnrolled", isEnrolled,
                "enrollment", enrollment != null ? enrollment : Map.of()
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse(e.getMessage()));
        }
    }
    
    /**
     * Get all attendees for an event (owner only)
     */
    @GetMapping("/{id}/attendees")
    public ResponseEntity<?> getEventAttendees(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            Authentication authentication) {
        try {
            Long userId = getUserIdFromAuthentication(authentication);
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new ErrorResponse("Authentication required"));
            }
            
            Pageable pageable = PageRequest.of(page, size);
            Page<EventAttendeeResponse> attendees = eventService.getEventAttendees(id, userId, pageable);
            return ResponseEntity.ok(attendees);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse(e.getMessage()));
        }
    }
    
    /**
     * Get user's enrolled events
     */
    @GetMapping("/my-enrollments")
    public ResponseEntity<?> getMyEnrollments(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {
        try {
            Long userId = getUserIdFromAuthentication(authentication);
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new ErrorResponse("Authentication required"));
            }
            
            Pageable pageable = PageRequest.of(page, size);
            Page<EventAttendeeResponse> enrollments = eventService.getUserEnrolledEvents(userId, pageable);
            return ResponseEntity.ok(enrollments);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse(e.getMessage()));
        }
    }
    
    /**
     * Check-in attendee by ticket code (owner only)
     */
    @PostMapping("/{id}/check-in")
    public ResponseEntity<?> checkInAttendee(
            @PathVariable Long id,
            @RequestBody Map<String, String> request,
            Authentication authentication) {
        try {
            Long userId = getUserIdFromAuthentication(authentication);
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new ErrorResponse("Authentication required"));
            }
            
            String ticketCode = request.get("ticketCode");
            if (ticketCode == null || ticketCode.isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(new ErrorResponse("Código do bilhete é obrigatório"));
            }
            
            EventAttendeeResponse response = eventService.checkInAttendee(id, ticketCode, userId);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse(e.getMessage()));
        }
    }
    
    private Long getUserIdFromAuthentication(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        String email = userDetails.getUsername();
        
        return userRepository.findByEmail(email)
                .map(User::getId)
                .orElse(null);
    }
    
    private boolean isAdmin(Long userId) {
        return userRepository.findById(userId)
                .map(user -> "ADMIN".equals(user.getRule()))
                .orElse(false);
    }
    
    // Helper classes
    @lombok.Data
    @lombok.AllArgsConstructor
    private static class ErrorResponse {
        private String message;
    }
    
    @lombok.Data
    @lombok.AllArgsConstructor
    private static class MessageResponse {
        private String message;
    }
}

