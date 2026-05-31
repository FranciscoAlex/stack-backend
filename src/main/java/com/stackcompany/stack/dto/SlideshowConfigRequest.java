package com.stackcompany.stack.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SlideshowConfigRequest {
    private String pageKey;
    private String imageUrl;
    private Integer displayOrder;
    private String title;
    private String subtitle;
    private Boolean enabled;
}

