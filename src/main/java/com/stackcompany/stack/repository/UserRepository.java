package com.stackcompany.stack.repository;

import com.stackcompany.stack.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    Optional<User> findByActivationToken(String activationToken);
    Optional<User> findByPasswordResetToken(String passwordResetToken);
    
    // Stripe customer lookup
    Optional<User> findByStripeCustomerId(String stripeCustomerId);

    // Bot / admin lookup
    Optional<User> findFirstByRule(String rule);
    java.util.List<User> findAllByRule(String rule);

    // Statistics queries
    long countByRule(String rule);
    
    @Query("SELECT COUNT(u) FROM User u WHERE u.createdAt >= :since")
    long countByCreatedAtAfter(@Param("since") LocalDateTime since);
    
    @Query("SELECT COUNT(u) FROM User u WHERE u.updatedAt >= :since")
    long countByUpdatedAtAfter(@Param("since") LocalDateTime since);
}

