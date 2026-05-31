package com.stackcompany.stack.repository;

import com.stackcompany.stack.entity.Event;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface EventRepository extends JpaRepository<Event, Long> {
    
    // Find all published and approved events, ordered by event date
    @Query("SELECT e FROM Event e WHERE e.isPublished = true AND e.status = 'APPROVED' AND e.eventDate >= :now ORDER BY e.eventDate ASC")
    Page<Event> findPublishedUpcomingEvents(@Param("now") LocalDateTime now, Pageable pageable);
    
    // Find all published and approved events (including past)
    @Query("SELECT e FROM Event e WHERE e.isPublished = true AND e.status = 'APPROVED' ORDER BY e.eventDate DESC")
    Page<Event> findPublishedEvents(Pageable pageable);
    
    // Find events by user
    Page<Event> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
    
    // Find events by status
    Page<Event> findByStatusOrderByCreatedAtDesc(String status, Pageable pageable);
    
    // Find events by category
    @Query("SELECT e FROM Event e WHERE e.isPublished = true AND e.status = 'APPROVED' AND e.category = :category AND e.eventDate >= :now ORDER BY e.eventDate ASC")
    Page<Event> findPublishedEventsByCategory(@Param("category") String category, @Param("now") LocalDateTime now, Pageable pageable);
    
    // Find events by status and published flag
    @Query("SELECT e FROM Event e WHERE e.status = :status AND e.isPublished = :isPublished ORDER BY e.createdAt DESC")
    Page<Event> findByStatusAndIsPublished(@Param("status") String status, @Param("isPublished") Boolean isPublished, Pageable pageable);
}

