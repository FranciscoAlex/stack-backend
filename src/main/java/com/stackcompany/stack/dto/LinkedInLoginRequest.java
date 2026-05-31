package com.stackcompany.stack.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LinkedInLoginRequest {
    @NotBlank(message = "LinkedIn authorization code is required")
    private String code;
}

