package com.stackcompany.stack.controller;

import com.stackcompany.stack.dto.PostResponse;
import com.stackcompany.stack.entity.Post;
import com.stackcompany.stack.entity.User;
import com.stackcompany.stack.repository.PostRepository;
import com.stackcompany.stack.repository.UserRepository;
import com.stackcompany.stack.service.PostService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/meta")
@CrossOrigin(origins = "*")
public class MetaTagsController {
    
    @Autowired
    private PostRepository postRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private PostService postService;
    
    @Value("${app.url:https://stakesomosholdrs.com}")
    private String appUrl;
    
    @GetMapping(value = "/post/{id}", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> getPostMetaTags(@PathVariable Long id) {
        try {
            Post post = postRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Post not found"));
            
            // Only show APPROVED posts to crawlers
            if (!"APPROVED".equals(post.getStatus())) {
                return getDefaultHtml();
            }
            
            // Get post image
            List<String> imageUrls = post.getImageUrls();
            String imageUrl = imageUrls != null && !imageUrls.isEmpty() 
                    ? imageUrls.get(0) 
                    : (post.getImageUrl() != null ? post.getImageUrl() : "");
            
            // Get author name
            User author = post.getUser();
            String authorName = (author.getFirstName() != null ? author.getFirstName() : "") +
                    (author.getLastName() != null ? " " + author.getLastName() : "").trim();
            if (authorName.isEmpty()) {
                authorName = author.getEmail() != null ? author.getEmail().split("@")[0] : "Usuário";
            }
            
            // Prepare meta tag values
            String title = post.getContent();
            if (title != null && title.length() > 60) {
                title = title.substring(0, 60) + "...";
            }
            if (title == null || title.isEmpty()) {
                title = "Post no Stack";
            }
            
            String description = "Post por " + authorName + " no Stack";
            String url = appUrl + "/post/" + id;
            
            return ResponseEntity.ok(buildHtmlWithMetaTags(title, description, imageUrl, url, "article"));
        } catch (Exception e) {
            return getDefaultHtml();
        }
    }
    
    @GetMapping(value = "/profile/{id}", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> getProfileMetaTags(@PathVariable Long id) {
        try {
            User user = userRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            
            // Get user name
            String firstName = user.getFirstName() != null ? user.getFirstName() : "";
            String lastName = user.getLastName() != null ? user.getLastName() : "";
            String fullName = (firstName + " " + lastName).trim();
            if (fullName.isEmpty()) {
                fullName = user.getEmail() != null ? user.getEmail() : "Usuário";
            }
            
            // Get description
            String description = user.getAbout() != null && !user.getAbout().isEmpty() 
                    ? user.getAbout() 
                    : (fullName + " no Stack");
            
            // Get image (avatar or cover)
            String imageUrl = user.getAvatarUrl() != null && !user.getAvatarUrl().isEmpty()
                    ? user.getAvatarUrl()
                    : (user.getCoverImageUrl() != null && !user.getCoverImageUrl().isEmpty()
                            ? user.getCoverImageUrl()
                            : "");
            
            String url = appUrl + "/profile/" + id;
            
            return ResponseEntity.ok(buildHtmlWithMetaTags(fullName, description, imageUrl, url, "profile"));
        } catch (Exception e) {
            return getDefaultHtml();
        }
    }
    
    private String buildHtmlWithMetaTags(String title, String description, String imageUrl, String url, String type) {
        // Escape HTML entities
        title = escapeHtml(title);
        description = escapeHtml(description);
        imageUrl = escapeHtml(imageUrl);
        url = escapeHtml(url);
        
        // Build complete HTML with meta tags
        StringBuilder html = new StringBuilder();
        html.append("<!doctype html>\n");
        html.append("<html lang=\"pt\">\n");
        html.append("  <head>\n");
        html.append("    <meta charset=\"UTF-8\" />\n");
        html.append("    <link rel=\"icon\" type=\"image/png\" href=\"/Icon.png\" />\n");
        html.append("    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0, viewport-fit=cover\" />\n");
        html.append("    <title>").append(title).append(" - Stack</title>\n");
        html.append("    <meta name=\"description\" content=\"").append(description).append("\" />\n");
        html.append("    <meta property=\"og:title\" content=\"").append(title).append("\" />\n");
        html.append("    <meta property=\"og:description\" content=\"").append(description).append("\" />\n");
        if (imageUrl != null && !imageUrl.isEmpty()) {
            html.append("    <meta property=\"og:image\" content=\"").append(imageUrl).append("\" />\n");
        }
        html.append("    <meta property=\"og:url\" content=\"").append(url).append("\" />\n");
        html.append("    <meta property=\"og:type\" content=\"").append(type).append("\" />\n");
        html.append("    <meta property=\"og:site_name\" content=\"Stack\" />\n");
        html.append("    <meta name=\"twitter:card\" content=\"summary_large_image\" />\n");
        html.append("    <meta name=\"twitter:title\" content=\"").append(title).append("\" />\n");
        html.append("    <meta name=\"twitter:description\" content=\"").append(description).append("\" />\n");
        if (imageUrl != null && !imageUrl.isEmpty()) {
            html.append("    <meta name=\"twitter:image\" content=\"").append(imageUrl).append("\" />\n");
        }
        html.append("    <link rel=\"preconnect\" href=\"https://images.unsplash.com\" crossorigin />\n");
        html.append("    <script>window.location.href=\"").append(url).append("\";</script>\n");
        html.append("  </head>\n");
        html.append("  <body>\n");
        html.append("    <div id=\"root\"></div>\n");
        html.append("    <script type=\"module\" src=\"/src/main.tsx\"></script>\n");
        html.append("  </body>\n");
        html.append("</html>\n");
        
        return html.toString();
    }
    
    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;")
                   .replace("'", "&#39;");
    }
    
    private ResponseEntity<String> getDefaultHtml() {
        // Return default HTML with basic meta tags
        String html = buildHtmlWithMetaTags(
            "Stack - Plataforma social para profissionais",
            "Stack - Plataforma social para profissionais",
            "",
            appUrl,
            "website"
        );
        return ResponseEntity.ok(html);
    }
}

