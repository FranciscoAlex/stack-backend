package com.stackcompany.stack.controller;

import com.stackcompany.stack.dto.*;
import com.stackcompany.stack.entity.User;
import com.stackcompany.stack.service.PaymentService;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.net.Webhook;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@Slf4j
public class PaymentController {
    
    private final PaymentService paymentService;
    
    @Value("${stripe.webhook-secret:}")
    private String webhookSecret;
    
    /**
     * Create a Stripe Checkout Session for subscription
     * POST /api/payments/create-checkout-session
     */
    @PostMapping("/create-checkout-session")
    public ResponseEntity<?> createCheckoutSession(
            @AuthenticationPrincipal User user,
            @RequestBody CreateCheckoutSessionRequest request) {
        
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("message", "Authentication required"));
        }
        
        try {
            CreateCheckoutSessionResponse response = paymentService.createCheckoutSession(user, request);
            return ResponseEntity.ok(response);
        } catch (StripeException e) {
            log.error("Stripe error creating checkout session: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("message", "Error creating payment session: " + e.getMessage()));
        } catch (Exception e) {
            log.error("Error creating checkout session: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("message", "Internal server error"));
        }
    }
    
    /**
     * Get current subscription status
     * GET /api/payments/subscription-status
     */
    @GetMapping("/subscription-status")
    public ResponseEntity<?> getSubscriptionStatus(@AuthenticationPrincipal User user) {
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("message", "Authentication required"));
        }
        
        try {
            SubscriptionStatusResponse status = paymentService.getSubscriptionStatus(user);
            return ResponseEntity.ok(status);
        } catch (StripeException e) {
            log.error("Stripe error getting subscription status: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("message", "Error getting subscription status: " + e.getMessage()));
        } catch (Exception e) {
            log.error("Error getting subscription status: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("message", "Internal server error"));
        }
    }
    
    /**
     * Create a Stripe Billing Portal session
     * POST /api/payments/billing-portal
     */
    @PostMapping("/billing-portal")
    public ResponseEntity<?> createBillingPortal(
            @AuthenticationPrincipal User user,
            @RequestBody BillingPortalRequest request) {
        
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("message", "Authentication required"));
        }
        
        try {
            BillingPortalResponse response = paymentService.createBillingPortalSession(user, request.getReturnUrl());
            return ResponseEntity.ok(response);
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("message", e.getMessage()));
        } catch (StripeException e) {
            log.error("Stripe error creating billing portal: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("message", "Error creating billing portal: " + e.getMessage()));
        } catch (Exception e) {
            log.error("Error creating billing portal: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("message", "Internal server error"));
        }
    }
    
    /**
     * Cancel subscription at period end
     * POST /api/payments/cancel-subscription
     */
    @PostMapping("/cancel-subscription")
    public ResponseEntity<?> cancelSubscription(@AuthenticationPrincipal User user) {
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("message", "Authentication required"));
        }
        
        try {
            SubscriptionStatusResponse status = paymentService.cancelSubscription(user);
            return ResponseEntity.ok(status);
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("message", e.getMessage()));
        } catch (StripeException e) {
            log.error("Stripe error cancelling subscription: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("message", "Error cancelling subscription: " + e.getMessage()));
        } catch (Exception e) {
            log.error("Error cancelling subscription: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("message", "Internal server error"));
        }
    }
    
    /**
     * Resume a cancelled subscription
     * POST /api/payments/resume-subscription
     */
    @PostMapping("/resume-subscription")
    public ResponseEntity<?> resumeSubscription(@AuthenticationPrincipal User user) {
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("message", "Authentication required"));
        }
        
        try {
            SubscriptionStatusResponse status = paymentService.resumeSubscription(user);
            return ResponseEntity.ok(status);
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("message", e.getMessage()));
        } catch (StripeException e) {
            log.error("Stripe error resuming subscription: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("message", "Error resuming subscription: " + e.getMessage()));
        } catch (Exception e) {
            log.error("Error resuming subscription: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("message", "Internal server error"));
        }
    }
    
    /**
     * Handle Stripe webhooks
     * POST /api/payments/webhook
     */
    @PostMapping("/webhook")
    public ResponseEntity<?> handleWebhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String sigHeader) {
        
        Event event;
        
        try {
            // Verify webhook signature if secret is configured
            if (webhookSecret != null && !webhookSecret.isEmpty()) {
                event = Webhook.constructEvent(payload, sigHeader, webhookSecret);
            } else {
                // For development without webhook signing
                event = Event.GSON.fromJson(payload, Event.class);
                log.warn("Webhook signature verification skipped - webhook secret not configured");
            }
        } catch (SignatureVerificationException e) {
            log.error("Invalid webhook signature: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("message", "Invalid signature"));
        } catch (Exception e) {
            log.error("Error parsing webhook: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("message", "Error parsing webhook"));
        }
        
        try {
            paymentService.handleWebhookEvent(event);
            return ResponseEntity.ok(Map.of("received", true));
        } catch (Exception e) {
            log.error("Error handling webhook event: {}", e.getMessage());
            // Return 200 to prevent Stripe from retrying
            return ResponseEntity.ok(Map.of("received", true, "error", e.getMessage()));
        }
    }
}

