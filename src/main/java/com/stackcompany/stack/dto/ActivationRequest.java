package com.stackcompany.stack.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ActivationRequest {
    @NotBlank(message = "Activation token is required")
    private String token;
}

