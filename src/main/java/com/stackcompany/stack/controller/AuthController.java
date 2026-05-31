package com.stackcompany.stack.controller;

import com.stackcompany.stack.dto.ActivationRequest;
import com.stackcompany.stack.dto.AuthResponse;
import com.stackcompany.stack.dto.GoogleLoginRequest;
import com.stackcompany.stack.dto.LinkedInLoginRequest;
import com.stackcompany.stack.dto.LoginRequest;
import com.stackcompany.stack.dto.PasswordResetConfirmRequest;
import com.stackcompany.stack.dto.PasswordResetRequest;
import com.stackcompany.stack.dto.RefreshTokenRequest;
import com.stackcompany.stack.dto.RegisterRequest;
import com.stackcompany.stack.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {
    
    @Autowired
    private AuthService authService;
    
    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        try {
            authService.register(request);
            // Registration throws exception with success message to prevent login
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(new MessageResponse("Account created successfully. Please check your email to activate your account."));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse(e.getMessage()));
        }
    }
    
    @PostMapping("/activate")
    public ResponseEntity<?> activateAccount(@Valid @RequestBody ActivationRequest request) {
        try {
            authService.activateAccount(request.getToken());
            return ResponseEntity.ok(new MessageResponse("Account activated successfully. You can now login."));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse(e.getMessage()));
        }
    }
    
    @PostMapping("/resend-activation")
    public ResponseEntity<?> resendActivation(@Valid @RequestBody PasswordResetRequest request) {
        try {
            authService.resendActivationEmail(request.getEmail());
            return ResponseEntity.ok(new MessageResponse("Activation email sent. Please check your email."));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse(e.getMessage()));
        }
    }
    
    @PostMapping("/password-reset/request")
    public ResponseEntity<?> requestPasswordReset(@Valid @RequestBody PasswordResetRequest request) {
        try {
            authService.requestPasswordReset(request.getEmail());
            return ResponseEntity.ok(new MessageResponse("Password reset email sent. Please check your email."));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse(e.getMessage()));
        }
    }
    
    @PostMapping("/password-reset/confirm")
    public ResponseEntity<?> confirmPasswordReset(@Valid @RequestBody PasswordResetConfirmRequest request) {
        try {
            authService.resetPassword(request.getToken(), request.getNewPassword());
            return ResponseEntity.ok(new MessageResponse("Password reset successfully. You can now login with your new password."));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse(e.getMessage()));
        }
    }
    
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        try {
            AuthResponse response = authService.login(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse("Invalid email or password"));
        }
    }
    
    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        try {
            AuthResponse response = authService.refreshToken(request);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse(e.getMessage()));
        }
    }
    
    @PostMapping("/logout")
    public ResponseEntity<?> logout(@Valid @RequestBody RefreshTokenRequest request) {
        try {
            authService.logout(request.getRefreshToken());
            return ResponseEntity.ok(new MessageResponse("Logged out successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse(e.getMessage()));
        }
    }
    
    @PostMapping("/google")
    public ResponseEntity<?> loginWithGoogle(@Valid @RequestBody GoogleLoginRequest request) {
        try {
            System.out.println("========================================");
            System.out.println("Google OAuth Request Received");
            System.out.println("Request access token present: " + (request != null && request.getAccessToken() != null));
            System.out.println("Access token length: " + (request != null && request.getAccessToken() != null ? request.getAccessToken().length() : 0));
            System.out.println("========================================");
            
            if (request == null || request.getAccessToken() == null || request.getAccessToken().isEmpty()) {
                System.err.println("ERROR: Google access token is missing or empty");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(new ErrorResponse("Google access token is required"));
            }
            AuthResponse response = authService.loginWithGoogle(request.getAccessToken());
            System.out.println("Google OAuth successful for user: " + response.getEmail());
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            // Log the error for debugging with full stack trace
            System.err.println("========================================");
            System.err.println("Google OAuth Authentication Error");
            System.err.println("Error: " + e.getMessage());
            System.err.println("========================================");
            e.printStackTrace();
            System.err.println("========================================");
            
            // Return detailed error message to help with debugging
            String errorMessage = e.getMessage();
            if (errorMessage == null || errorMessage.isEmpty()) {
                errorMessage = "Authentication failed. Please check server logs for details.";
            }
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse(errorMessage));
        } catch (Exception e) {
            // Log the error for debugging
            System.err.println("========================================");
            System.err.println("Google OAuth Unexpected Error");
            System.err.println("Error: " + e.getMessage());
            System.err.println("========================================");
            e.printStackTrace();
            System.err.println("========================================");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Failed to authenticate with Google: " + e.getMessage()));
        }
    }
    
    @PostMapping("/linkedin")
    public ResponseEntity<?> loginWithLinkedIn(@Valid @RequestBody LinkedInLoginRequest request) {
        try {
            AuthResponse response = authService.loginWithLinkedIn(request.getCode());
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Failed to authenticate with LinkedIn"));
        }
    }
    
    // Helper classes for responses
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

