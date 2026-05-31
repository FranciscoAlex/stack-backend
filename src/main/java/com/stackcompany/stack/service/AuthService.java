package com.stackcompany.stack.service;

import com.stackcompany.stack.dto.AuthResponse;
import com.stackcompany.stack.dto.LoginRequest;
import com.stackcompany.stack.dto.RegisterRequest;
import com.stackcompany.stack.dto.RefreshTokenRequest;
import com.stackcompany.stack.entity.User;
import com.stackcompany.stack.repository.UserRepository;
import com.stackcompany.stack.security.JwtTokenProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class AuthService {
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private RefreshTokenService refreshTokenService;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @Autowired
    private JwtTokenProvider jwtTokenProvider;
    
    @Autowired
    private AuthenticationManager authenticationManager;
    
    @Autowired
    private EmailService emailService;
    
    @Value("${google.client-id:}")
    private String googleClientId;
    
    @Value("${linkedin.client-id:}")
    private String linkedInClientId;
    
    @Value("${linkedin.client-secret:}")
    private String linkedInClientSecret;
    
    @Value("${linkedin.redirect-uri:}")
    private String linkedInRedirectUri;
    
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    @Transactional(rollbackFor = Exception.class)
    public AuthResponse loginWithGoogle(String accessToken) {
            try {
            System.out.println("Received Google access token, length: " + accessToken.length());
            System.out.println("Token starts with: " + (accessToken.length() > 10 ? accessToken.substring(0, 10) : accessToken));
                
            // Validate access token with Google API and get user info
            System.out.println("Validating Google access token with Google API...");
            GoogleUserInfo userInfo = validateGoogleAccessToken(accessToken);
                
            if (userInfo.email == null || userInfo.email.isEmpty()) {
                System.err.println("ERROR: Email not found in Google user info.");
                    throw new RuntimeException("Email not found in Google token. Please ensure your Google account has an email address.");
                }
                
                // Save user in a separate transaction to avoid commit issues
            User user = saveOrUpdateGoogleUser(userInfo.email, userInfo.firstName, userInfo.lastName, userInfo.pictureUrl);
                
                // Generate tokens (do this before sending email to avoid transaction issues)
            String jwtAccessToken = jwtTokenProvider.generateTokenWithEmail(user);
                String refreshToken = createRefreshToken(user);
                
                AuthResponse response = AuthResponse.builder()
                    .accessToken(jwtAccessToken)
                        .refreshToken(refreshToken)
                        .userId(user.getId())
                        .email(user.getEmail())
                        .firstName(user.getFirstName())
                        .lastName(user.getLastName())
                        .avatarUrl(user.getAvatarUrl())
                        .rule(user.getRule() != null ? user.getRule() : "USER")
                        .build();
                
                return response;
                        
            } catch (RuntimeException e) {
                System.err.println("RuntimeException in Google OAuth: " + e.getMessage());
                e.printStackTrace();
                throw e;
            } catch (Exception e) {
            System.err.println("ERROR: Failed to process Google access token");
                System.err.println("Exception type: " + e.getClass().getName());
                System.err.println("Exception message: " + e.getMessage());
                e.printStackTrace();
            throw new RuntimeException("Invalid Google access token: " + e.getMessage() + ". Please try logging in again.");
            }
    }
    
    /**
     * Validate Google access token and get user information
     */
    private GoogleUserInfo validateGoogleAccessToken(String accessToken) throws IOException, InterruptedException {
        // First, validate the token with Google
        HttpRequest tokenInfoRequest = HttpRequest.newBuilder()
                .uri(URI.create("https://www.googleapis.com/oauth2/v1/tokeninfo?access_token=" + accessToken))
                .GET()
                .build();
        
        HttpResponse<String> tokenInfoResponse = httpClient.send(tokenInfoRequest, HttpResponse.BodyHandlers.ofString());
        
        if (tokenInfoResponse.statusCode() != 200) {
            throw new RuntimeException("Invalid Google access token: " + tokenInfoResponse.body());
        }
        
        // Get user info from Google
        HttpRequest userInfoRequest = HttpRequest.newBuilder()
                .uri(URI.create("https://www.googleapis.com/oauth2/v2/userinfo"))
                .header("Authorization", "Bearer " + accessToken)
                .GET()
                .build();
        
        HttpResponse<String> userInfoResponse = httpClient.send(userInfoRequest, HttpResponse.BodyHandlers.ofString());
        
        if (userInfoResponse.statusCode() != 200) {
            throw new RuntimeException("Failed to get user info from Google: " + userInfoResponse.body());
        }
        
        JsonNode userInfoNode = objectMapper.readTree(userInfoResponse.body());
        
        GoogleUserInfo userInfo = new GoogleUserInfo();
        userInfo.email = userInfoNode.has("email") ? userInfoNode.get("email").asText() : null;
        userInfo.firstName = userInfoNode.has("given_name") ? userInfoNode.get("given_name").asText() : null;
        userInfo.lastName = userInfoNode.has("family_name") ? userInfoNode.get("family_name").asText() : null;
        userInfo.pictureUrl = userInfoNode.has("picture") && !userInfoNode.get("picture").isNull() 
                ? userInfoNode.get("picture").asText() 
                : null;
        
        return userInfo;
        }
    
    private static class GoogleUserInfo {
        String email;
        String firstName;
        String lastName;
        String pictureUrl;
    }
    
    /**
     * Save or update Google user in a separate transaction to avoid commit issues
     * Uses REQUIRES_NEW to ensure it commits independently
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    private User saveOrUpdateGoogleUser(String email, String firstName, String lastName, String pictureUrl) {
        User user = userRepository.findByEmail(email).orElse(null);
        
        if (user == null) {
            // Create new user from Google account
            user = User.builder()
                    .email(email)
                    .password(passwordEncoder.encode(java.util.UUID.randomUUID().toString())) // Random password for OAuth users
                    .firstName(firstName)
                    .lastName(lastName)
                    .avatarUrl(pictureUrl)
                    .enabled(true)
                    .build();
            
            user = userRepository.save(user);
        } else {
            // Update avatar if it's not set or if Google provides a new one
            if (pictureUrl != null && (user.getAvatarUrl() == null || user.getAvatarUrl().isEmpty())) {
                user.setAvatarUrl(pictureUrl);
                user = userRepository.save(user);
            }
        }
        
        return user;
    }
    
    @Transactional
    public void register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already exists");
        }
        
        // Generate activation token
        String activationToken = java.util.UUID.randomUUID().toString();
        LocalDateTime activationTokenExpiresAt = LocalDateTime.now().plusDays(1); // 24 hours
        
        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .gender(request.getGender())
                .dateOfBirth(request.getDateOfBirth())
                .enabled(false) // Account requires activation
                .activationToken(activationToken)
                .activationTokenExpiresAt(activationTokenExpiresAt)
                .build();
        
        user = userRepository.save(user);
        
        // Send activation email
        try {
            emailService.sendActivationEmail(user.getEmail(), user.getFirstName(), activationToken);
        } catch (Exception e) {
            // Log error but don't fail registration
            System.err.println("Failed to send activation email: " + e.getMessage());
        }
        
        // Method completes successfully - transaction will be committed
        // Controller will return success message to user
    }
    
    @Transactional
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Invalid email or password"));
        
        // Check if account is activated
        if (!user.isEnabled()) {
            throw new RuntimeException("Account not activated. Please check your email for the activation link.");
        }
        
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );
        
        // Generate token with email as subject (since we authenticate by email)
        String accessToken = jwtTokenProvider.generateTokenWithEmail(user);
        String refreshToken = createRefreshToken(user);
        
        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .userId(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .avatarUrl(user.getAvatarUrl())
                .rule(user.getRule() != null ? user.getRule() : "USER")
                .build();
    }
    
    @Transactional
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        String token = request.getRefreshToken();
        
        // Get user email from in-memory token store
        String userEmail = refreshTokenService.getUserEmail(token);
        if (userEmail == null) {
            throw new RuntimeException("Invalid or expired refresh token");
        }
        
        // Get user from database
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        // Remove old refresh token
        refreshTokenService.removeToken(token);
        
        // Generate new tokens with email as subject
        String newAccessToken = jwtTokenProvider.generateTokenWithEmail(user);
        String newRefreshToken = createRefreshToken(user);
        
        return AuthResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .userId(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .avatarUrl(user.getAvatarUrl())
                .rule(user.getRule() != null ? user.getRule() : "USER")
                .build();
    }
    
    @Transactional
    public AuthResponse loginWithLinkedIn(String code) {
        try {
            // Exchange authorization code for access token
            String accessToken = exchangeLinkedInCodeForToken(code);
            
            // Get user profile from LinkedIn
            LinkedInProfile profile = getLinkedInProfile(accessToken);
            
            if (profile.email == null) {
                throw new RuntimeException("Email not found in LinkedIn profile");
            }
            
            // Check if user exists
            User user = userRepository.findByEmail(profile.email).orElse(null);
            
            if (user == null) {
                // Create new user from LinkedIn account
                user = User.builder()
                        .email(profile.email)
                        .password(passwordEncoder.encode(java.util.UUID.randomUUID().toString())) // Random password for OAuth users
                        .firstName(profile.firstName)
                        .lastName(profile.lastName)
                        .enabled(true)
                        .build();
                
                user = userRepository.save(user);
            }
            
            // Generate tokens
            String jwtAccessToken = jwtTokenProvider.generateTokenWithEmail(user);
            String refreshToken = createRefreshToken(user);
            
            return AuthResponse.builder()
                    .accessToken(jwtAccessToken)
                    .refreshToken(refreshToken)
                    .userId(user.getId())
                    .email(user.getEmail())
                    .firstName(user.getFirstName())
                    .lastName(user.getLastName())
                    .avatarUrl(user.getAvatarUrl())
                    .rule(user.getRule() != null ? user.getRule() : "USER")
                    .build();
                    
        } catch (Exception e) {
            throw new RuntimeException("Failed to authenticate with LinkedIn: " + e.getMessage(), e);
        }
    }
    
    private String exchangeLinkedInCodeForToken(String code) throws IOException, InterruptedException {
        String requestBody = String.format(
            "grant_type=authorization_code&code=%s&redirect_uri=%s&client_id=%s&client_secret=%s",
            code,
            linkedInRedirectUri,
            linkedInClientId,
            linkedInClientSecret
        );
        
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://www.linkedin.com/oauth/v2/accessToken"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();
        
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        
        if (response.statusCode() != 200) {
            throw new RuntimeException("Failed to exchange LinkedIn code for token: " + response.body());
        }
        
        JsonNode jsonNode = objectMapper.readTree(response.body());
        return jsonNode.get("access_token").asText();
    }
    
    private LinkedInProfile getLinkedInProfile(String accessToken) throws IOException, InterruptedException {
        // Get basic profile
        HttpRequest profileRequest = HttpRequest.newBuilder()
                .uri(URI.create("https://api.linkedin.com/v2/userinfo"))
                .header("Authorization", "Bearer " + accessToken)
                .GET()
                .build();
        
        HttpResponse<String> profileResponse = httpClient.send(profileRequest, HttpResponse.BodyHandlers.ofString());
        
        if (profileResponse.statusCode() != 200) {
            throw new RuntimeException("Failed to get LinkedIn profile: " + profileResponse.body());
        }
        
        JsonNode profileNode = objectMapper.readTree(profileResponse.body());
        
        LinkedInProfile profile = new LinkedInProfile();
        profile.email = profileNode.has("email") ? profileNode.get("email").asText() : null;
        profile.firstName = profileNode.has("given_name") ? profileNode.get("given_name").asText() : null;
        profile.lastName = profileNode.has("family_name") ? profileNode.get("family_name").asText() : null;
        
        return profile;
    }
    
    private static class LinkedInProfile {
        String email;
        String firstName;
        String lastName;
    }
    
    public void logout(String refreshToken) {
        refreshTokenService.removeToken(refreshToken);
    }
    
    private String createRefreshToken(User user) {
        // Generate refresh token with email as subject
        Map<String, Object> claims = new HashMap<>();
        claims.put("type", "refresh");
        String token = jwtTokenProvider.createTokenWithSubject(claims, user.getEmail(), jwtTokenProvider.getRefreshExpiration());
        LocalDateTime expiresAt = LocalDateTime.now().plusDays(7);
        
        // Store token in memory
        refreshTokenService.storeToken(token, user.getEmail(), expiresAt);
        
        return token;
    }
    
    @Transactional
    public void activateAccount(String activationToken) {
        // Trim and decode token to handle any whitespace or URL encoding
        String cleanToken = activationToken != null ? activationToken.trim() : null;
        
        if (cleanToken == null || cleanToken.isEmpty()) {
            throw new RuntimeException("Activation token is required");
        }
        
        // Try to URL decode in case the token was encoded (though it shouldn't be for UUID)
        try {
            cleanToken = java.net.URLDecoder.decode(cleanToken, "UTF-8").trim();
        } catch (java.io.UnsupportedEncodingException e) {
            // Should never happen with UTF-8, use token as-is
        }
        
        User user = userRepository.findByActivationToken(cleanToken)
                .orElseThrow(() -> new RuntimeException("Invalid activation token"));
        
        // Check if token is expired
        if (user.getActivationTokenExpiresAt() == null || 
            user.getActivationTokenExpiresAt().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Activation token has expired. Please request a new activation email.");
        }
        
        // Check if already activated
        if (user.isEnabled()) {
            throw new RuntimeException("Account is already activated");
        }
        
        // Activate account
        user.setEnabled(true);
        user.setActivationToken(null);
        user.setActivationTokenExpiresAt(null);
        userRepository.save(user);
    }
    
    @Transactional
    public void resendActivationEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        // Check if account is already activated
        if (user.isEnabled()) {
            throw new RuntimeException("Account is already activated. Please login to continue.");
        }
        
        // Generate new activation token
        String activationToken = java.util.UUID.randomUUID().toString();
        LocalDateTime activationTokenExpiresAt = LocalDateTime.now().plusDays(1); // 24 hours
        
        user.setActivationToken(activationToken);
        user.setActivationTokenExpiresAt(activationTokenExpiresAt);
        userRepository.save(user);
        
        // Send activation email
        try {
            emailService.sendActivationEmail(user.getEmail(), user.getFirstName(), activationToken);
        } catch (Exception e) {
            throw new RuntimeException("Failed to send activation email: " + e.getMessage(), e);
        }
    }
    
    @Transactional
    public void requestPasswordReset(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        // Generate password reset token
        String resetToken = java.util.UUID.randomUUID().toString();
        LocalDateTime resetTokenExpiresAt = LocalDateTime.now().plusHours(1); // 1 hour
        
        user.setPasswordResetToken(resetToken);
        user.setPasswordResetTokenExpiresAt(resetTokenExpiresAt);
        userRepository.save(user);
        
        // Send password reset email
        try {
            emailService.sendPasswordResetEmail(user.getEmail(), user.getFirstName(), resetToken);
        } catch (Exception e) {
            throw new RuntimeException("Failed to send password reset email: " + e.getMessage(), e);
        }
    }
    
    @Transactional
    public void resetPassword(String resetToken, String newPassword) {
        User user = userRepository.findByPasswordResetToken(resetToken)
                .orElseThrow(() -> new RuntimeException("Invalid password reset token"));
        
        // Check if token is expired
        if (user.getPasswordResetTokenExpiresAt() == null || 
            user.getPasswordResetTokenExpiresAt().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Password reset token has expired. Please request a new password reset.");
        }
        
        // Update password
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setPasswordResetToken(null);
        user.setPasswordResetTokenExpiresAt(null);
        userRepository.save(user);
    }
}

