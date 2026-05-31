package com.stackcompany.stack.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BannerConfigRequest {
    private String pageKey;
    private String imageUrl;
    private String title;
    private String subtitle;
    private String linkUrl;
    private Boolean enabled;
}

