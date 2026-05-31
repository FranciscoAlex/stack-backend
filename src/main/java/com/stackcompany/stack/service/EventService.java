package com.stackcompany.stack.service;

import com.stackcompany.stack.dto.EventAttendeeResponse;
import com.stackcompany.stack.dto.EventRequest;
import com.stackcompany.stack.dto.EventResponse;
import com.stackcompany.stack.entity.Event;
import com.stackcompany.stack.entity.EventAttendee;
import com.stackcompany.stack.entity.User;
import com.stackcompany.stack.repository.EventAttendeeRepository;
import com.stackcompany.stack.repository.EventRepository;
import com.stackcompany.stack.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class EventService {
    
    @Autowired
    private EventRepository eventRepository;
    
    @Autowired
    private EventAttendeeRepository eventAttendeeRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private EmailService emailService;
    
    private static final String TICKET_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final SecureRandom RANDOM = new SecureRandom();
    
    /**
     * Create and publish an event
     * This is the most important function to publish an event
     */
    @Transactional
    public EventResponse publishEvent(Long userId, EventRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        // Set status based on user role: ADMIN events are auto-approved, USER events need approval
        String status = "ADMIN".equals(user.getRule()) ? "APPROVED" : "PENDING";
        
        // Validate event date is in the future
        if (request.getEventDate() != null && request.getEventDate().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Event date must be in the future");
        }
        
        // Validate end date is after start date if provided
        if (request.getEndDate() != null && request.getEventDate() != null 
                && request.getEndDate().isBefore(request.getEventDate())) {
            throw new RuntimeException("End date must be after event date");
        }
        
        Event event = Event.builder()
                .user(user)
                .title(request.getTitle())
                .description(request.getDescription())
                .location(request.getLocation())
                .eventDate(request.getEventDate())
                .endDate(request.getEndDate())
                .imageUrl(request.getImageUrl())
                .category(request.getCategory())
                .maxAttendees(request.getMaxAttendees())
                .currentAttendees(0)
                .price(request.getPrice() != null ? request.getPrice() : BigDecimal.ZERO)
                .isOnline(request.getIsOnline() != null ? request.getIsOnline() : false)
                .onlineLink(request.getOnlineLink())
                .status(status)
                .isPublished(true) // Mark as published when created
                .build();
        
        event = eventRepository.save(event);
        eventRepository.flush();
        
        return convertToResponse(event);
    }
    
    /**
     * Create an event without publishing (draft)
     */
    @Transactional
    public EventResponse createEvent(Long userId, EventRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        // Set status based on user role: ADMIN events are auto-approved, USER events need approval
        String status = "ADMIN".equals(user.getRule()) ? "APPROVED" : "PENDING";
        
        Event event = Event.builder()
                .user(user)
                .title(request.getTitle())
                .description(request.getDescription())
                .location(request.getLocation())
                .eventDate(request.getEventDate())
                .endDate(request.getEndDate())
                .imageUrl(request.getImageUrl())
                .category(request.getCategory())
                .maxAttendees(request.getMaxAttendees())
                .currentAttendees(0)
                .price(request.getPrice() != null ? request.getPrice() : BigDecimal.ZERO)
                .isOnline(request.getIsOnline() != null ? request.getIsOnline() : false)
                .onlineLink(request.getOnlineLink())
                .status(status)
                .isPublished(false) // Draft, not published
                .build();
        
        event = eventRepository.save(event);
        eventRepository.flush();
        
        return convertToResponse(event);
    }
    
    /**
     * Publish an existing event (change from draft to published)
     */
    @Transactional
    public EventResponse publishExistingEvent(Long eventId, Long userId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Event not found"));
        
        if (!event.getUser().getId().equals(userId)) {
            throw new RuntimeException("You don't have permission to publish this event");
        }
        
        if (!"APPROVED".equals(event.getStatus()) && !"PENDING".equals(event.getStatus())) {
            throw new RuntimeException("Only approved or pending events can be published");
        }
        
        event.setIsPublished(true);
        event = eventRepository.save(event);
        
        return convertToResponse(event);
    }
    
    /**
     * Unpublish an event (change from published to draft)
     */
    @Transactional
    public EventResponse unpublishEvent(Long eventId, Long userId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Event not found"));
        
        if (!event.getUser().getId().equals(userId)) {
            throw new RuntimeException("You don't have permission to unpublish this event");
        }
        
        event.setIsPublished(false);
        event = eventRepository.save(event);
        
        return convertToResponse(event);
    }
    
    /**
     * Get all published upcoming events
     */
    public Page<EventResponse> getPublishedUpcomingEvents(Pageable pageable) {
        Page<Event> events = eventRepository.findPublishedUpcomingEvents(LocalDateTime.now(), pageable);
        return events.map(this::convertToResponse);
    }
    
    /**
     * Get all published events (including past)
     */
    public Page<EventResponse> getPublishedEvents(Pageable pageable) {
        Page<Event> events = eventRepository.findPublishedEvents(pageable);
        return events.map(this::convertToResponse);
    }
    
    /**
     * Get events by user
     */
    public Page<EventResponse> getUserEvents(Long userId, Pageable pageable) {
        Page<Event> events = eventRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
        return events.map(this::convertToResponse);
    }
    
    /**
     * Get event by ID
     */
    public EventResponse getEventById(Long eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Event not found"));
        
        // Only return published and approved events for non-owners
        // (This check should be done in controller based on current user)
        return convertToResponse(event);
    }
    
    /**
     * Get event by ID with user enrollment status
     */
    public EventResponse getEventById(Long eventId, Long userId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Event not found"));
        
        return convertToResponseWithEnrollment(event, userId);
    }
    
    /**
     * Update an event
     */
    @Transactional
    public EventResponse updateEvent(Long eventId, Long userId, EventRequest request) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Event not found"));
        
        if (!event.getUser().getId().equals(userId)) {
            throw new RuntimeException("You don't have permission to update this event");
        }
        
        // Validate event date is in the future if being changed
        if (request.getEventDate() != null && request.getEventDate().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Event date must be in the future");
        }
        
        // Validate end date is after start date if provided
        if (request.getEndDate() != null && request.getEventDate() != null 
                && request.getEndDate().isBefore(request.getEventDate())) {
            throw new RuntimeException("End date must be after event date");
        }
        
        event.setTitle(request.getTitle());
        event.setDescription(request.getDescription());
        event.setLocation(request.getLocation());
        if (request.getEventDate() != null) {
            event.setEventDate(request.getEventDate());
        }
        event.setEndDate(request.getEndDate());
        event.setImageUrl(request.getImageUrl());
        event.setCategory(request.getCategory());
        event.setMaxAttendees(request.getMaxAttendees());
        if (request.getPrice() != null) {
            event.setPrice(request.getPrice());
        }
        if (request.getIsOnline() != null) {
            event.setIsOnline(request.getIsOnline());
        }
        event.setOnlineLink(request.getOnlineLink());
        
        event = eventRepository.save(event);
        
        return convertToResponse(event);
    }
    
    /**
     * Delete an event
     */
    @Transactional
    public void deleteEvent(Long eventId, Long userId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Event not found"));
        
        if (!event.getUser().getId().equals(userId)) {
            throw new RuntimeException("You don't have permission to delete this event");
        }
        
        eventRepository.delete(event);
    }
    
    /**
     * Approve an event (admin only)
     */
    @Transactional
    public EventResponse approveEvent(Long eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Event not found"));
        
        if (!"PENDING".equals(event.getStatus())) {
            throw new RuntimeException("Only pending events can be approved");
        }
        
        event.setStatus("APPROVED");
        event = eventRepository.save(event);
        
        return convertToResponse(event);
    }
    
    /**
     * Reject an event (admin only)
     */
    @Transactional
    public EventResponse rejectEvent(Long eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Event not found"));
        
        if (!"PENDING".equals(event.getStatus())) {
            throw new RuntimeException("Only pending events can be rejected");
        }
        
        event.setStatus("REJECTED");
        event.setIsPublished(false); // Unpublish rejected events
        event = eventRepository.save(event);
        
        return convertToResponse(event);
    }
    
    /**
     * Get pending events for admin review
     */
    public Page<EventResponse> getPendingEvents(Pageable pageable) {
        Page<Event> events = eventRepository.findByStatusOrderByCreatedAtDesc("PENDING", pageable);
        return events.map(this::convertToResponse);
    }
    
    /**
     * Enroll a user in an event
     */
    @Transactional
    public EventAttendeeResponse enrollInEvent(Long eventId, Long userId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Evento não encontrado"));
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilizador não encontrado"));
        
        // Check if event is published and approved
        if (!event.getIsPublished() || !"APPROVED".equals(event.getStatus())) {
            throw new RuntimeException("Este evento não está disponível para inscrições");
        }
        
        // Check if event is in the future
        if (event.getEventDate().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Não é possível inscrever-se em eventos passados");
        }
        
        // Check if user is already enrolled
        if (eventAttendeeRepository.existsByEventIdAndUserId(eventId, userId)) {
            throw new RuntimeException("Já está inscrito neste evento");
        }
        
        // Check if event has reached max capacity
        if (event.getMaxAttendees() != null && event.getMaxAttendees() > 0) {
            int confirmedCount = eventAttendeeRepository.countConfirmedByEventId(eventId);
            if (confirmedCount >= event.getMaxAttendees()) {
                throw new RuntimeException("Este evento já atingiu a capacidade máxima de participantes");
            }
        }
        
        // Generate unique ticket code
        String ticketCode = generateTicketCode();
        
        // Create enrollment
        EventAttendee attendee = EventAttendee.builder()
                .event(event)
                .user(user)
                .ticketCode(ticketCode)
                .status("CONFIRMED")
                .build();
        
        attendee = eventAttendeeRepository.save(attendee);
        
        // Update current attendees count
        event.setCurrentAttendees(event.getCurrentAttendees() + 1);
        eventRepository.save(event);
        
        // Send ticket email
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd 'de' MMMM 'de' yyyy 'às' HH:mm", 
                new java.util.Locale("pt", "PT"));
            String eventDateFormatted = event.getEventDate().format(formatter);
            String priceStr = event.getPrice() != null ? event.getPrice().toString() : "0";
            
            emailService.sendEventTicketEmail(
                user.getEmail(),
                user.getFirstName(),
                event.getTitle(),
                eventDateFormatted,
                event.getLocation(),
                ticketCode,
                event.getIsOnline() != null && event.getIsOnline(),
                event.getOnlineLink(),
                priceStr
            );
        } catch (Exception e) {
            // Log error but don't fail the enrollment
            System.err.println("Failed to send ticket email: " + e.getMessage());
        }
        
        return convertToAttendeeResponse(attendee);
    }
    
    /**
     * Cancel enrollment
     */
    @Transactional
    public void cancelEnrollment(Long eventId, Long userId) {
        EventAttendee attendee = eventAttendeeRepository.findByEventIdAndUserId(eventId, userId)
                .orElseThrow(() -> new RuntimeException("Inscrição não encontrada"));
        
        if ("CANCELLED".equals(attendee.getStatus())) {
            throw new RuntimeException("Esta inscrição já foi cancelada");
        }
        
        if ("CHECKED_IN".equals(attendee.getStatus())) {
            throw new RuntimeException("Não é possível cancelar uma inscrição após o check-in");
        }
        
        attendee.setStatus("CANCELLED");
        eventAttendeeRepository.save(attendee);
        
        // Update current attendees count
        Event event = attendee.getEvent();
        if (event.getCurrentAttendees() > 0) {
            event.setCurrentAttendees(event.getCurrentAttendees() - 1);
            eventRepository.save(event);
        }
    }
    
    /**
     * Check if user is enrolled in an event
     */
    public boolean isUserEnrolled(Long eventId, Long userId) {
        return eventAttendeeRepository.hasConfirmedEnrollment(eventId, userId);
    }
    
    /**
     * Get user's enrollment for an event
     */
    public EventAttendeeResponse getUserEnrollment(Long eventId, Long userId) {
        EventAttendee attendee = eventAttendeeRepository.findByEventIdAndUserId(eventId, userId)
                .orElse(null);
        return attendee != null ? convertToAttendeeResponse(attendee) : null;
    }
    
    /**
     * Get all attendees for an event (event owner only)
     */
    public Page<EventAttendeeResponse> getEventAttendees(Long eventId, Long userId, Pageable pageable) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Evento não encontrado"));
        
        // Only event owner can see all attendees
        if (!event.getUser().getId().equals(userId)) {
            throw new RuntimeException("Não tem permissão para ver os participantes deste evento");
        }
        
        Page<EventAttendee> attendees = eventAttendeeRepository.findByEventIdOrderByCreatedAtDesc(eventId, pageable);
        return attendees.map(this::convertToAttendeeResponse);
    }
    
    /**
     * Get user's enrolled events
     */
    public Page<EventAttendeeResponse> getUserEnrolledEvents(Long userId, Pageable pageable) {
        Page<EventAttendee> attendees = eventAttendeeRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
        return attendees.map(this::convertToAttendeeResponse);
    }
    
    /**
     * Check-in attendee (event owner only)
     */
    @Transactional
    public EventAttendeeResponse checkInAttendee(Long eventId, String ticketCode, Long userId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Evento não encontrado"));
        
        // Only event owner can check-in attendees
        if (!event.getUser().getId().equals(userId)) {
            throw new RuntimeException("Não tem permissão para fazer check-in neste evento");
        }
        
        EventAttendee attendee = eventAttendeeRepository.findByTicketCode(ticketCode)
                .orElseThrow(() -> new RuntimeException("Bilhete não encontrado"));
        
        if (!attendee.getEvent().getId().equals(eventId)) {
            throw new RuntimeException("Este bilhete não pertence a este evento");
        }
        
        if ("CANCELLED".equals(attendee.getStatus())) {
            throw new RuntimeException("Esta inscrição foi cancelada");
        }
        
        if ("CHECKED_IN".equals(attendee.getStatus())) {
            throw new RuntimeException("Este participante já fez check-in");
        }
        
        attendee.setStatus("CHECKED_IN");
        attendee.setCheckedInAt(LocalDateTime.now());
        attendee = eventAttendeeRepository.save(attendee);
        
        return convertToAttendeeResponse(attendee);
    }
    
    /**
     * Generate unique ticket code
     */
    private String generateTicketCode() {
        StringBuilder code = new StringBuilder("STK-");
        for (int i = 0; i < 8; i++) {
            code.append(TICKET_CHARS.charAt(RANDOM.nextInt(TICKET_CHARS.length())));
        }
        
        // Ensure uniqueness
        while (eventAttendeeRepository.findByTicketCode(code.toString()).isPresent()) {
            code = new StringBuilder("STK-");
            for (int i = 0; i < 8; i++) {
                code.append(TICKET_CHARS.charAt(RANDOM.nextInt(TICKET_CHARS.length())));
            }
        }
        
        return code.toString();
    }
    
    /**
     * Convert EventAttendee entity to EventAttendeeResponse DTO
     */
    private EventAttendeeResponse convertToAttendeeResponse(EventAttendee attendee) {
        EventAttendeeResponse.AttendeeInfo attendeeInfo = EventAttendeeResponse.AttendeeInfo.builder()
                .id(attendee.getUser().getId())
                .email(attendee.getUser().getEmail())
                .firstName(attendee.getUser().getFirstName())
                .lastName(attendee.getUser().getLastName())
                .avatar(attendee.getUser().getAvatarUrl())
                .build();
        
        return EventAttendeeResponse.builder()
                .id(attendee.getId())
                .eventId(attendee.getEvent().getId())
                .eventTitle(attendee.getEvent().getTitle())
                .eventDate(attendee.getEvent().getEventDate())
                .eventLocation(attendee.getEvent().getLocation())
                .isOnline(attendee.getEvent().getIsOnline())
                .onlineLink(attendee.getEvent().getOnlineLink())
                .ticketCode(attendee.getTicketCode())
                .status(attendee.getStatus())
                .checkedInAt(attendee.getCheckedInAt())
                .createdAt(attendee.getCreatedAt())
                .attendee(attendeeInfo)
                .build();
    }
    
    /**
     * Convert Event entity to EventResponse DTO
     */
    private EventResponse convertToResponse(Event event) {
        return convertToResponseWithEnrollment(event, null);
    }
    
    /**
     * Convert Event entity to EventResponse DTO with enrollment status
     */
    private EventResponse convertToResponseWithEnrollment(Event event, Long userId) {
        EventResponse.AuthorInfo authorInfo = EventResponse.AuthorInfo.builder()
                .id(event.getUser().getId())
                .email(event.getUser().getEmail())
                .firstName(event.getUser().getFirstName())
                .lastName(event.getUser().getLastName())
                .avatar(event.getUser().getAvatarUrl())
                .build();
        
        // Calculate available seats
        Integer availableSeats = null;
        if (event.getMaxAttendees() != null && event.getMaxAttendees() > 0) {
            availableSeats = event.getMaxAttendees() - (event.getCurrentAttendees() != null ? event.getCurrentAttendees() : 0);
            if (availableSeats < 0) availableSeats = 0;
        }
        
        // Check enrollment status
        Boolean isEnrolled = false;
        String userTicketCode = null;
        if (userId != null) {
            EventAttendee enrollment = eventAttendeeRepository.findByEventIdAndUserId(event.getId(), userId).orElse(null);
            if (enrollment != null && "CONFIRMED".equals(enrollment.getStatus())) {
                isEnrolled = true;
                userTicketCode = enrollment.getTicketCode();
            }
        }
        
        return EventResponse.builder()
                .id(event.getId())
                .title(event.getTitle())
                .description(event.getDescription())
                .location(event.getLocation())
                .eventDate(event.getEventDate())
                .endDate(event.getEndDate())
                .imageUrl(event.getImageUrl())
                .category(event.getCategory())
                .maxAttendees(event.getMaxAttendees())
                .currentAttendees(event.getCurrentAttendees())
                .availableSeats(availableSeats)
                .price(event.getPrice())
                .isOnline(event.getIsOnline())
                .onlineLink(event.getOnlineLink())
                .status(event.getStatus())
                .isPublished(event.getIsPublished())
                .isEnrolled(isEnrolled)
                .userTicketCode(userTicketCode)
                .createdAt(event.getCreatedAt())
                .updatedAt(event.getUpdatedAt())
                .author(authorInfo)
                .build();
    }
}

