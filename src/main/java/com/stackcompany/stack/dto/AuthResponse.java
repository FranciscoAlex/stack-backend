package com.stackcompany.stack.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthResponse {
    private String accessToken;
    private String refreshToken;
    @Builder.Default
    private String tokenType = "Bearer";
    private Long userId;
    private String email;
    private String firstName;
    private String lastName;
    private String avatarUrl;
    private String rule; // 'ADMIN' or 'USER'
}

