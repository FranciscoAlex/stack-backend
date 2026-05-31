package com.stackcompany.stack.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class VoteRequest {
    @NotBlank(message = "Vote type is required")
    private String voteType; // 'up' or 'down' or 'remove'
}

