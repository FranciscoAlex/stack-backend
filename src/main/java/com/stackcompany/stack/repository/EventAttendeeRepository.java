package com.stackcompany.stack.repository;

import com.stackcompany.stack.entity.EventAttendee;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EventAttendeeRepository extends JpaRepository<EventAttendee, Long> {
    
    // Check if user is already enrolled in event
    boolean existsByEventIdAndUserId(Long eventId, Long userId);
    
    // Find enrollment by event and user
    Optional<EventAttendee> findByEventIdAndUserId(Long eventId, Long userId);
    
    // Find enrollment by ticket code
    Optional<EventAttendee> findByTicketCode(String ticketCode);
    
    // Find all attendees for an event
    Page<EventAttendee> findByEventIdOrderByCreatedAtDesc(Long eventId, Pageable pageable);
    
    // Find all events a user is enrolled in
    Page<EventAttendee> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
    
    // Count confirmed attendees for an event
    @Query("SELECT COUNT(ea) FROM EventAttendee ea WHERE ea.event.id = :eventId AND ea.status = 'CONFIRMED'")
    int countConfirmedByEventId(@Param("eventId") Long eventId);
    
    // Find all confirmed attendees for an event
    @Query("SELECT ea FROM EventAttendee ea WHERE ea.event.id = :eventId AND ea.status = 'CONFIRMED' ORDER BY ea.createdAt DESC")
    List<EventAttendee> findConfirmedByEventId(@Param("eventId") Long eventId);
    
    // Check if user has a confirmed enrollment
    @Query("SELECT COUNT(ea) > 0 FROM EventAttendee ea WHERE ea.event.id = :eventId AND ea.user.id = :userId AND ea.status = 'CONFIRMED'")
    boolean hasConfirmedEnrollment(@Param("eventId") Long eventId, @Param("userId") Long userId);
}

