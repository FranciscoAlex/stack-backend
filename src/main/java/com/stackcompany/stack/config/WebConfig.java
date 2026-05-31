package com.stackcompany.stack.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final ResourceLoader resourceLoader;

    public WebConfig(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    /**
     * Configure static resource handling for frontend files
     * Only enabled if frontend files are present (for backend-only deployments, this will be skipped)
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        if (hasStaticResources()) {
            registry.addResourceHandler("/**")
                    .addResourceLocations("classpath:/static/")
                    .resourceChain(false);
        }
    }

    /**
     * Configure view controllers for SPA routing
     * This ensures that React Router routes are handled correctly
     * Only enabled if frontend files are present (for backend-only deployments, this will be skipped)
     */
    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        if (hasStaticResources()) {
            // Forward all non-API routes to index.html for React Router
            registry.addViewController("/").setViewName("forward:/index.html");
            registry.addViewController("/{path:[^\\.]*}").setViewName("forward:/index.html");
        }
    }

    /**
     * Check if static resources (frontend files) are available
     * Returns false for backend-only deployments where static files are removed
     */
    private boolean hasStaticResources() {
        try {
            Resource resource = resourceLoader.getResource("classpath:/static/index.html");
            return resource.exists();
        } catch (Exception e) {
            return false;
        }
    }
}

