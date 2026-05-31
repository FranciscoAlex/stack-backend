package com.stackcompany.stack.service;

import com.stackcompany.stack.dto.*;
import com.stackcompany.stack.entity.User;
import com.stackcompany.stack.repository.UserRepository;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.*;
import com.stripe.model.billingportal.Session;
import com.stripe.param.*;
import com.stripe.param.billingportal.SessionCreateParams;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {
    
    private final UserRepository userRepository;
    
    @Value("${stripe.secret-key}")
    private String stripeSecretKey;
    
    @Value("${stripe.success-url}")
    private String defaultSuccessUrl;
    
    @Value("${stripe.cancel-url}")
    private String defaultCancelUrl;
    
    // Stripe Price IDs - these should be configured in your Stripe Dashboard
    // Using environment variables for flexibility
    @Value("${stripe.price.weekly:price_weekly}")
    private String weeklyPriceId;
    
    @Value("${stripe.price.monthly:price_monthly}")
    private String monthlyPriceId;
    
    @Value("${stripe.price.annual:price_annual}")
    private String annualPriceId;
    
    @PostConstruct
    public void init() {
        Stripe.apiKey = stripeSecretKey;
        log.info("Stripe API initialized");
    }
    
    /**
     * Create a Stripe Checkout Session for subscription
     */
    @Transactional
    public CreateCheckoutSessionResponse createCheckoutSession(User user, CreateCheckoutSessionRequest request) throws StripeException {
        log.info("Creating checkout session for user: {} with plan: {}", user.getEmail(), request.getPlanType());
        
        // Get or create Stripe customer
        String customerId = getOrCreateCustomer(user);
        
        // Determine the price ID based on plan type
        String priceId = getPriceIdForPlan(request.getPlanType(), request.getPriceId());
        
        // Build the checkout session
        com.stripe.param.checkout.SessionCreateParams params = com.stripe.param.checkout.SessionCreateParams.builder()
            .setMode(com.stripe.param.checkout.SessionCreateParams.Mode.SUBSCRIPTION)
            .setCustomer(customerId)
            .setSuccessUrl(request.getSuccessUrl() != null ? request.getSuccessUrl() : defaultSuccessUrl)
            .setCancelUrl(request.getCancelUrl() != null ? request.getCancelUrl() : defaultCancelUrl)
            .addLineItem(
                com.stripe.param.checkout.SessionCreateParams.LineItem.builder()
                    .setPrice(priceId)
                    .setQuantity(1L)
                    .build()
            )
            // Add subscription data for trial period (7 days free trial)
            .setSubscriptionData(
                com.stripe.param.checkout.SessionCreateParams.SubscriptionData.builder()
                    .setTrialPeriodDays(7L)
                    .putMetadata("plan_type", request.getPlanType())
                    .putMetadata("user_id", user.getId().toString())
                    .build()
            )
            // Allow promotion codes
            .setAllowPromotionCodes(true)
            // Set billing address collection
            .setBillingAddressCollection(com.stripe.param.checkout.SessionCreateParams.BillingAddressCollection.AUTO)
            // Add metadata
            .putMetadata("user_id", user.getId().toString())
            .putMetadata("user_email", user.getEmail())
            .putMetadata("plan_type", request.getPlanType())
            .build();
        
        com.stripe.model.checkout.Session session = com.stripe.model.checkout.Session.create(params);
        
        log.info("Checkout session created: {}", session.getId());
        
        return CreateCheckoutSessionResponse.builder()
            .sessionId(session.getId())
            .url(session.getUrl())
            .build();
    }
    
    /**
     * Get current subscription status for a user
     */
    public SubscriptionStatusResponse getSubscriptionStatus(User user) throws StripeException {
        if (user.getStripeCustomerId() == null) {
            return SubscriptionStatusResponse.builder()
                .isActive(false)
                .build();
        }
        
        // List active subscriptions for the customer
        SubscriptionListParams params = SubscriptionListParams.builder()
            .setCustomer(user.getStripeCustomerId())
            .setStatus(SubscriptionListParams.Status.ALL)
            .setLimit(1L)
            .build();
        
        SubscriptionCollection subscriptions = Subscription.list(params);
        
        if (subscriptions.getData().isEmpty()) {
            return SubscriptionStatusResponse.builder()
                .isActive(false)
                .customerId(user.getStripeCustomerId())
                .build();
        }
        
        Subscription subscription = subscriptions.getData().get(0);
        String planType = subscription.getMetadata().get("plan_type");
        
        LocalDateTime periodEnd = LocalDateTime.ofInstant(
            Instant.ofEpochSecond(subscription.getCurrentPeriodEnd()),
            ZoneId.systemDefault()
        );
        
        boolean isActive = "active".equals(subscription.getStatus()) || 
                          "trialing".equals(subscription.getStatus());
        
        return SubscriptionStatusResponse.builder()
            .isActive(isActive)
            .planType(planType)
            .currentPeriodEnd(periodEnd)
            .cancelAtPeriodEnd(subscription.getCancelAtPeriodEnd())
            .customerId(user.getStripeCustomerId())
            .subscriptionId(subscription.getId())
            .status(subscription.getStatus())
            .build();
    }
    
    /**
     * Create a billing portal session for managing subscription
     */
    public BillingPortalResponse createBillingPortalSession(User user, String returnUrl) throws StripeException {
        if (user.getStripeCustomerId() == null) {
            throw new IllegalStateException("User does not have a Stripe customer ID");
        }
        
        SessionCreateParams params = SessionCreateParams.builder()
            .setCustomer(user.getStripeCustomerId())
            .setReturnUrl(returnUrl)
            .build();
        
        Session session = Session.create(params);
        
        return BillingPortalResponse.builder()
            .url(session.getUrl())
            .build();
    }
    
    /**
     * Cancel subscription at period end
     */
    @Transactional
    public SubscriptionStatusResponse cancelSubscription(User user) throws StripeException {
        if (user.getStripeSubscriptionId() == null) {
            throw new IllegalStateException("User does not have an active subscription");
        }
        
        Subscription subscription = Subscription.retrieve(user.getStripeSubscriptionId());
        
        SubscriptionUpdateParams params = SubscriptionUpdateParams.builder()
            .setCancelAtPeriodEnd(true)
            .build();
        
        subscription = subscription.update(params);
        
        // Update user's subscription status
        user.setSubscriptionCancelAtPeriodEnd(true);
        userRepository.save(user);
        
        log.info("Subscription cancelled at period end for user: {}", user.getEmail());
        
        return getSubscriptionStatus(user);
    }
    
    /**
     * Resume a cancelled subscription
     */
    @Transactional
    public SubscriptionStatusResponse resumeSubscription(User user) throws StripeException {
        if (user.getStripeSubscriptionId() == null) {
            throw new IllegalStateException("User does not have a subscription");
        }
        
        Subscription subscription = Subscription.retrieve(user.getStripeSubscriptionId());
        
        if (!subscription.getCancelAtPeriodEnd()) {
            throw new IllegalStateException("Subscription is not set to cancel");
        }
        
        SubscriptionUpdateParams params = SubscriptionUpdateParams.builder()
            .setCancelAtPeriodEnd(false)
            .build();
        
        subscription = subscription.update(params);
        
        // Update user's subscription status
        user.setSubscriptionCancelAtPeriodEnd(false);
        userRepository.save(user);
        
        log.info("Subscription resumed for user: {}", user.getEmail());
        
        return getSubscriptionStatus(user);
    }
    
    /**
     * Handle Stripe webhook events
     */
    @Transactional
    public void handleWebhookEvent(Event event) {
        log.info("Processing webhook event: {}", event.getType());
        
        switch (event.getType()) {
            case "checkout.session.completed":
                handleCheckoutSessionCompleted(event);
                break;
            case "customer.subscription.created":
            case "customer.subscription.updated":
                handleSubscriptionUpdated(event);
                break;
            case "customer.subscription.deleted":
                handleSubscriptionDeleted(event);
                break;
            case "invoice.payment_succeeded":
                handleInvoicePaymentSucceeded(event);
                break;
            case "invoice.payment_failed":
                handleInvoicePaymentFailed(event);
                break;
            default:
                log.info("Unhandled webhook event type: {}", event.getType());
        }
    }
    
    // ==================== Private Helper Methods ====================
    
    private String getOrCreateCustomer(User user) throws StripeException {
        // If user already has a Stripe customer ID, return it
        if (user.getStripeCustomerId() != null) {
            try {
                // Verify the customer still exists
                Customer.retrieve(user.getStripeCustomerId());
                return user.getStripeCustomerId();
            } catch (StripeException e) {
                log.warn("Stripe customer not found, creating new one for user: {}", user.getEmail());
            }
        }
        
        // Create a new Stripe customer
        CustomerCreateParams params = CustomerCreateParams.builder()
            .setEmail(user.getEmail())
            .setName(user.getFirstName() + " " + user.getLastName())
            .putMetadata("user_id", user.getId().toString())
            .build();
        
        Customer customer = Customer.create(params);
        
        // Save the customer ID to the user
        user.setStripeCustomerId(customer.getId());
        userRepository.save(user);
        
        log.info("Created Stripe customer: {} for user: {}", customer.getId(), user.getEmail());
        
        return customer.getId();
    }
    
    private String getPriceIdForPlan(String planType, String providedPriceId) {
        // If a specific price ID was provided, use it
        if (providedPriceId != null && !providedPriceId.isEmpty() && !providedPriceId.contains("placeholder")) {
            return providedPriceId;
        }
        
        // Otherwise, use the configured price IDs
        return switch (planType.toLowerCase()) {
            case "weekly" -> weeklyPriceId;
            case "monthly" -> monthlyPriceId;
            case "annual" -> annualPriceId;
            default -> throw new IllegalArgumentException("Invalid plan type: " + planType);
        };
    }
    
    private void handleCheckoutSessionCompleted(Event event) {
        com.stripe.model.checkout.Session session = (com.stripe.model.checkout.Session) event.getDataObjectDeserializer()
            .getObject().orElse(null);
        
        if (session == null) {
            log.error("Failed to deserialize checkout session from event");
            return;
        }
        
        String userId = session.getMetadata().get("user_id");
        if (userId == null) {
            log.error("No user_id in checkout session metadata");
            return;
        }
        
        userRepository.findById(Long.parseLong(userId)).ifPresent(user -> {
            user.setStripeCustomerId(session.getCustomer());
            user.setStripeSubscriptionId(session.getSubscription());
            user.setSubscriptionStatus("active");
            user.setSubscriptionPlanType(session.getMetadata().get("plan_type"));
            userRepository.save(user);
            
            log.info("Checkout completed for user: {}, subscription: {}", user.getEmail(), session.getSubscription());
        });
    }
    
    private void handleSubscriptionUpdated(Event event) {
        Subscription subscription = (Subscription) event.getDataObjectDeserializer()
            .getObject().orElse(null);
        
        if (subscription == null) {
            log.error("Failed to deserialize subscription from event");
            return;
        }
        
        String customerId = subscription.getCustomer();
        userRepository.findByStripeCustomerId(customerId).ifPresent(user -> {
            user.setStripeSubscriptionId(subscription.getId());
            user.setSubscriptionStatus(subscription.getStatus());
            user.setSubscriptionCancelAtPeriodEnd(subscription.getCancelAtPeriodEnd());
            
            if (subscription.getCurrentPeriodEnd() != null) {
                user.setSubscriptionCurrentPeriodEnd(
                    LocalDateTime.ofInstant(
                        Instant.ofEpochSecond(subscription.getCurrentPeriodEnd()),
                        ZoneId.systemDefault()
                    )
                );
            }
            
            String planType = subscription.getMetadata().get("plan_type");
            if (planType != null) {
                user.setSubscriptionPlanType(planType);
            }
            
            userRepository.save(user);
            log.info("Subscription updated for user: {}, status: {}", user.getEmail(), subscription.getStatus());
        });
    }
    
    private void handleSubscriptionDeleted(Event event) {
        Subscription subscription = (Subscription) event.getDataObjectDeserializer()
            .getObject().orElse(null);
        
        if (subscription == null) {
            log.error("Failed to deserialize subscription from event");
            return;
        }
        
        String customerId = subscription.getCustomer();
        userRepository.findByStripeCustomerId(customerId).ifPresent(user -> {
            user.setSubscriptionStatus("canceled");
            user.setStripeSubscriptionId(null);
            user.setSubscriptionPlanType(null);
            user.setSubscriptionCurrentPeriodEnd(null);
            user.setSubscriptionCancelAtPeriodEnd(false);
            userRepository.save(user);
            
            log.info("Subscription deleted for user: {}", user.getEmail());
        });
    }
    
    private void handleInvoicePaymentSucceeded(Event event) {
        Invoice invoice = (Invoice) event.getDataObjectDeserializer()
            .getObject().orElse(null);
        
        if (invoice == null) {
            log.error("Failed to deserialize invoice from event");
            return;
        }
        
        log.info("Invoice payment succeeded: {}", invoice.getId());
    }
    
    private void handleInvoicePaymentFailed(Event event) {
        Invoice invoice = (Invoice) event.getDataObjectDeserializer()
            .getObject().orElse(null);
        
        if (invoice == null) {
            log.error("Failed to deserialize invoice from event");
            return;
        }
        
        String customerId = invoice.getCustomer();
        userRepository.findByStripeCustomerId(customerId).ifPresent(user -> {
            user.setSubscriptionStatus("past_due");
            userRepository.save(user);
            
            log.warn("Invoice payment failed for user: {}", user.getEmail());
        });
    }
}

