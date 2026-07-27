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

    // Up to 20: a PDF upload is converted to one image per page and the dialog
    // accepts PDFs of up to 20 pages. A cap of 5 here rejected every longer PDF.
    @Size(max = 20, message = "Máximo de 20 imagens permitidas")
    private List<@Size(max = 500, message = "Cada URL de imagem não pode exceder 500 caracteres") String> imageUrls;

    @Size(max = 5, message = "Máximo de 5 tags permitidas")
    private List<@Size(max = 30, message = "Cada tag não pode exceder 30 caracteres") String> tags;
}
