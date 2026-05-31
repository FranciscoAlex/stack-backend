package com.stackcompany.stack.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateCheckoutSessionRequest {
    private String planType; // 'weekly', 'monthly', 'annual'
    private String priceId;
    private String successUrl;
    private String cancelUrl;
}

