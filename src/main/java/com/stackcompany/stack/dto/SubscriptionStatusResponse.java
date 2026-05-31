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
public class SubscriptionStatusResponse {
    private boolean isActive;
    private String planType;
    private LocalDateTime currentPeriodEnd;
    private boolean cancelAtPeriodEnd;
    private String customerId;
    private String subscriptionId;
    private String status; // 'active', 'canceled', 'incomplete', 'past_due', 'trialing', 'unpaid'
}

