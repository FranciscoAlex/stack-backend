package com.stackcompany.stack.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.util.List;

@Data
public class PostRequest {
    @NotBlank(message = "Content is required")
    @Size(max = 10000, message = "Content must not exceed 10000 characters")
    private String content;

    // Support both single imageUrl (for backward compatibility) and multiple
    // imageUrls
    @Size(max = 500, message = "Image URL must not exceed 500 characters")
    private String imageUrl; // Deprecated, use imageUrls instead

    @Size(max = 5, message = "Maximum 5 images allowed")
    private List<@Size(max = 500, message = "Each image URL must not exceed 500 characters") String> imageUrls;

    @Size(max = 5, message = "Maximum 5 tags allowed")
    private List<@Size(max = 30, message = "Tag must not exceed 30 characters") String> tags;
}
