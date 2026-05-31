package com.stackcompany.stack.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User implements UserDetails {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(unique = true, nullable = false)
    private String email;
    
    @Column(nullable = false)
    private String password;
    
    @Column(name = "first_name")
    private String firstName;
    
    @Column(name = "last_name")
    private String lastName;
    
    @Column(name = "avatar_url")
    private String avatarUrl;
    
    @Column(name = "cover_image_url")
    private String coverImageUrl;
    
    @Column(name = "gender", length = 20)
    private String gender; // 'MALE', 'FEMALE', 'OTHER', or null
    
    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;
    
    @Column(name = "headline", length = 500)
    private String headline;
    
    @Column(name = "company", length = 255)
    private String company;
    
    @Column(name = "role", length = 255)
    private String role;
    
    @Column(name = "about", columnDefinition = "TEXT")
    private String about;
    
    @Column(name = "location", length = 255)
    private String location;
    
    @Column(name = "website", length = 500)
    private String website;
    
    @Column(name = "twitter", length = 255)
    private String twitter;
    
    @Column(name = "linkedin", length = 500)
    private String linkedin;
    
    // Investment fields for BODIVAS
    @Column(name = "bodivas_company_code", length = 10)
    private String bodivasCompanyCode;
    
    @Column(name = "bodivas_company_name", length = 255)
    private String bodivasCompanyName;
    
    @Column(name = "shares_owned")
    private Long sharesOwned;
    
    @Column(name = "share_percentage", precision = 5, scale = 2)
    private java.math.BigDecimal sharePercentage;
    
    @Column(name = "investment_amount", precision = 15, scale = 2)
    private java.math.BigDecimal investmentAmount;
    
    @Column(name = "entry_date")
    private LocalDate entryDate;
    
    @Column(name = "current_value", precision = 15, scale = 2)
    private java.math.BigDecimal currentValue;
    
    @Column(name = "average_price", precision = 10, scale = 2)
    private java.math.BigDecimal averagePrice;
    
    @Column(nullable = false, columnDefinition = "BOOLEAN DEFAULT FALSE")
    @Builder.Default
    private boolean enabled = false; // Changed to false - requires activation
    
    @Column(nullable = false, length = 20)
    @Builder.Default
    private String rule = "USER"; // 'ADMIN' or 'USER'
    
    @Column(name = "activation_token", length = 255)
    private String activationToken;
    
    @Column(name = "activation_token_expires_at")
    private LocalDateTime activationTokenExpiresAt;
    
    @Column(name = "password_reset_token", length = 255)
    private String passwordResetToken;
    
    @Column(name = "password_reset_token_expires_at")
    private LocalDateTime passwordResetTokenExpiresAt;
    
    // Stripe subscription fields
    @Column(name = "stripe_customer_id", length = 255)
    private String stripeCustomerId;
    
    @Column(name = "stripe_subscription_id", length = 255)
    private String stripeSubscriptionId;
    
    @Column(name = "subscription_status", length = 50)
    private String subscriptionStatus; // 'active', 'canceled', 'incomplete', 'past_due', 'trialing', 'unpaid'
    
    @Column(name = "subscription_plan_type", length = 50)
    private String subscriptionPlanType; // 'weekly', 'monthly', 'annual'
    
    @Column(name = "subscription_current_period_end")
    private LocalDateTime subscriptionCurrentPeriodEnd;
    
    @Column(name = "subscription_cancel_at_period_end")
    @Builder.Default
    private Boolean subscriptionCancelAtPeriodEnd = false;
    
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    // Override getUsername to return email since we authenticate by email
    // This is required for UserDetails interface
    @Override
    public String getUsername() {
        return email;
    }
    
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        String role = "ROLE_" + (rule != null ? rule : "USER");
        return List.of(new SimpleGrantedAuthority(role));
    }
    
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }
    
    @Override
    public boolean isAccountNonLocked() {
        return true;
    }
    
    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }
    
    @Override
    public boolean isEnabled() {
        return enabled;
    }
}

