package com.stackcompany.stack.controller;

import com.stackcompany.stack.dto.PostRequest;
import com.stackcompany.stack.dto.PostResponse;
import com.stackcompany.stack.dto.VoteRequest;
import com.stackcompany.stack.entity.Post;
import com.stackcompany.stack.entity.User;
import com.stackcompany.stack.repository.PostRepository;
import com.stackcompany.stack.repository.UserRepository;
import com.stackcompany.stack.service.PostService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/posts")
@CrossOrigin(origins = "*")
public class PostController {

    @Autowired
    private PostService postService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PostRepository postRepository;

    @PostMapping
    public ResponseEntity<?> createPost(@Valid @RequestBody PostRequest request, Authentication authentication) {
        try {
            Long userId = getUserIdFromAuthentication(authentication);
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new ErrorResponse("Authentication required"));
            }
            PostResponse response = postService.createPost(userId, request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse(e.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<Page<PostResponse>> getAllPosts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {
        Long userId = getUserIdFromAuthentication(authentication);
        Pageable pageable = PageRequest.of(page, size);
        Page<PostResponse> posts = postService.getAllPosts(pageable, userId);
        return ResponseEntity.ok(posts);
    }

    @GetMapping("/popular")
    public ResponseEntity<Page<PostResponse>> getPopularPosts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {
        Long userId = getUserIdFromAuthentication(authentication);
        Pageable pageable = PageRequest.of(page, size);
        Page<PostResponse> posts = postService.getPopularPosts(pageable, userId);
        return ResponseEntity.ok(posts);
    }

    @GetMapping("/tag/{tagName}")
    public ResponseEntity<Page<PostResponse>> getPostsByTag(
            @PathVariable String tagName,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {
        Long userId = getUserIdFromAuthentication(authentication);
        Pageable pageable = PageRequest.of(page, size);
        Page<PostResponse> posts = postService.getPostsByTag(tagName, pageable, userId);
        return ResponseEntity.ok(posts);
    }

    @GetMapping("/following")
    public ResponseEntity<Page<PostResponse>> getFollowingPosts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {
        Long userId = getUserIdFromAuthentication(authentication);
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        Pageable pageable = PageRequest.of(page, size);
        Page<PostResponse> posts = postService.getFollowingPosts(pageable, userId);
        return ResponseEntity.ok(posts);
    }

    // Admin endpoints - must come before /{id} to avoid path matching conflicts
    @GetMapping("/admin/pending")
    public ResponseEntity<?> getPendingPosts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {
        try {
            Long userId = getUserIdFromAuthentication(authentication);
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new ErrorResponse("Authentication required"));
            }

            if (!isAdmin(userId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(new ErrorResponse("Admin access required"));
            }

            Pageable pageable = PageRequest.of(page, size);
            Page<PostResponse> posts = postService.getPendingPosts(pageable, userId);
            return ResponseEntity.ok(posts);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse(e.getMessage()));
        }
    }

    @GetMapping("/admin/statistics")
    public ResponseEntity<?> getContentStatistics(Authentication authentication) {
        try {
            Long userId = getUserIdFromAuthentication(authentication);
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new ErrorResponse("Authentication required"));
            }

            if (!isAdmin(userId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(new ErrorResponse("Admin access required"));
            }

            // Calculate time ranges
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime thirtyDaysAgo = now.minusDays(30);
            LocalDateTime sevenDaysAgo = now.minusDays(7);
            LocalDateTime todayStart = now.withHour(0).withMinute(0).withSecond(0).withNano(0);

            // Post counts by status
            long totalPosts = postRepository.count();
            long approvedPosts = postRepository.countByStatus("APPROVED");
            long pendingPosts = postRepository.countByStatus("PENDING");
            long rejectedPosts = postRepository.countByStatus("REJECTED");

            // Posts over time
            long postsToday = postRepository.countPostsSince(todayStart);
            long postsThisWeek = postRepository.countPostsSince(sevenDaysAgo);
            long postsThisMonth = postRepository.countPostsSince(thirtyDaysAgo);

            // Engagement totals
            Long totalViews = postRepository.sumTotalViews();
            Long totalLikes = postRepository.sumTotalVotes();
            Long totalComments = postRepository.sumTotalComments();
            Long totalShares = postRepository.sumTotalShares();

            // Average engagement
            Double avgViews = postRepository.averageViews();
            Double avgLikes = postRepository.averageVotes();
            Double avgComments = postRepository.averageComments();

            // Top posts
            Page<Post> mostViewedPage = postRepository.findMostViewedPosts(PageRequest.of(0, 1));
            Page<Post> mostLikedPage = postRepository.findMostLikedPosts(PageRequest.of(0, 1));
            Page<Post> mostCommentedPage = postRepository.findMostCommentedPosts(PageRequest.of(0, 1));

            Map<String, Object> statistics = new HashMap<>();

            // Post counts
            statistics.put("totalPosts", totalPosts);
            statistics.put("approvedPosts", approvedPosts);
            statistics.put("pendingPosts", pendingPosts);
            statistics.put("rejectedPosts", rejectedPosts);

            // Posts over time
            statistics.put("postsToday", postsToday);
            statistics.put("postsThisWeek", postsThisWeek);
            statistics.put("postsThisMonth", postsThisMonth);

            // Engagement totals
            statistics.put("totalViews", totalViews != null ? totalViews : 0);
            statistics.put("totalLikes", totalLikes != null ? totalLikes : 0);
            statistics.put("totalComments", totalComments != null ? totalComments : 0);
            statistics.put("totalShares", totalShares != null ? totalShares : 0);

            // Average engagement
            statistics.put("avgViews", avgViews != null ? Math.round(avgViews * 10.0) / 10.0 : 0);
            statistics.put("avgLikes", avgLikes != null ? Math.round(avgLikes * 10.0) / 10.0 : 0);
            statistics.put("avgComments", avgComments != null ? Math.round(avgComments * 10.0) / 10.0 : 0);

            // Most viewed post
            if (!mostViewedPage.isEmpty()) {
                Post mostViewed = mostViewedPage.getContent().get(0);
                Map<String, Object> mostViewedMap = new HashMap<>();
                mostViewedMap.put("id", mostViewed.getId());
                mostViewedMap.put("content", mostViewed.getContent().length() > 100
                        ? mostViewed.getContent().substring(0, 100) + "..."
                        : mostViewed.getContent());
                mostViewedMap.put("views", mostViewed.getViews());
                mostViewedMap.put("votes", mostViewed.getVotes());
                mostViewedMap.put("authorId", mostViewed.getUser().getId());
                mostViewedMap.put("authorName",
                        (mostViewed.getUser().getFirstName() != null ? mostViewed.getUser().getFirstName() : "") + " " +
                                (mostViewed.getUser().getLastName() != null ? mostViewed.getUser().getLastName() : ""));
                statistics.put("mostViewedPost", mostViewedMap);
            }

            // Most liked post
            if (!mostLikedPage.isEmpty()) {
                Post mostLiked = mostLikedPage.getContent().get(0);
                Map<String, Object> mostLikedMap = new HashMap<>();
                mostLikedMap.put("id", mostLiked.getId());
                mostLikedMap.put("content", mostLiked.getContent().length() > 100
                        ? mostLiked.getContent().substring(0, 100) + "..."
                        : mostLiked.getContent());
                mostLikedMap.put("views", mostLiked.getViews());
                mostLikedMap.put("votes", mostLiked.getVotes());
                mostLikedMap.put("authorId", mostLiked.getUser().getId());
                mostLikedMap.put("authorName",
                        (mostLiked.getUser().getFirstName() != null ? mostLiked.getUser().getFirstName() : "") + " " +
                                (mostLiked.getUser().getLastName() != null ? mostLiked.getUser().getLastName() : ""));
                statistics.put("mostLikedPost", mostLikedMap);
            }

            // Most commented post
            if (!mostCommentedPage.isEmpty()) {
                Post mostCommented = mostCommentedPage.getContent().get(0);
                Map<String, Object> mostCommentedMap = new HashMap<>();
                mostCommentedMap.put("id", mostCommented.getId());
                mostCommentedMap.put("content", mostCommented.getContent().length() > 100
                        ? mostCommented.getContent().substring(0, 100) + "..."
                        : mostCommented.getContent());
                mostCommentedMap.put("views", mostCommented.getViews());
                mostCommentedMap.put("votes", mostCommented.getVotes());
                mostCommentedMap.put("commentsCount", mostCommented.getCommentsCount());
                mostCommentedMap.put("authorId", mostCommented.getUser().getId());
                mostCommentedMap.put("authorName",
                        (mostCommented.getUser().getFirstName() != null ? mostCommented.getUser().getFirstName() : "")
                                + " " +
                                (mostCommented.getUser().getLastName() != null ? mostCommented.getUser().getLastName()
                                        : ""));
                statistics.put("mostCommentedPost", mostCommentedMap);
            }

            return ResponseEntity.ok(statistics);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Error fetching content statistics: " + e.getMessage()));
        }
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<?> approvePost(@PathVariable Long id, Authentication authentication) {
        try {
            Long userId = getUserIdFromAuthentication(authentication);
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new ErrorResponse("Authentication required"));
            }

            if (!isAdmin(userId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(new ErrorResponse("Admin access required"));
            }

            postService.approvePost(id);
            return ResponseEntity.ok(new MessageResponse("Post approved successfully"));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse(e.getMessage()));
        }
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<?> rejectPost(@PathVariable Long id,
            @RequestBody RejectRequest request,
            Authentication authentication) {
        try {
            Long userId = getUserIdFromAuthentication(authentication);
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new ErrorResponse("Authentication required"));
            }

            if (!isAdmin(userId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(new ErrorResponse("Admin access required"));
            }

            String reason = request != null && request.getReason() != null
                    ? request.getReason()
                    : "Your post does not meet our community guidelines.";

            postService.rejectPost(id, reason);
            return ResponseEntity.ok(new MessageResponse("Post rejected successfully"));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse(e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getPostById(@PathVariable Long id, Authentication authentication) {
        try {
            Long userId = getUserIdFromAuthentication(authentication);
            PostResponse response = postService.getPostById(id, userId);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponse(e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updatePost(@PathVariable Long id,
            @Valid @RequestBody PostRequest request,
            Authentication authentication) {
        try {
            Long userId = getUserIdFromAuthentication(authentication);
            PostResponse response = postService.updatePost(id, userId, request);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse(e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deletePost(@PathVariable Long id, Authentication authentication) {
        try {
            Long userId = getUserIdFromAuthentication(authentication);
            postService.deletePost(id, userId);
            return ResponseEntity.ok(new MessageResponse("Post deleted successfully"));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse(e.getMessage()));
        }
    }

    @PostMapping("/{id}/vote")
    public ResponseEntity<?> votePost(@PathVariable Long id,
            @Valid @RequestBody VoteRequest request,
            Authentication authentication) {
        try {
            Long userId = getUserIdFromAuthentication(authentication);
            postService.votePost(id, userId, request.getVoteType());
            return ResponseEntity.ok().build();
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse(e.getMessage()));
        }
    }

    @PostMapping("/{id}/bookmark")
    public ResponseEntity<?> toggleBookmark(@PathVariable Long id,
            Authentication authentication) {
        try {
            Long userId = getUserIdFromAuthentication(authentication);
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new ErrorResponse("Authentication required"));
            }
            postService.toggleBookmark(id, userId);
            return ResponseEntity.ok().build();
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse(e.getMessage()));
        }
    }

    @PostMapping("/{id}/view")
    public ResponseEntity<?> incrementViews(@PathVariable Long id) {
        try {
            postService.incrementViews(id);
            return ResponseEntity.ok().build();
        } catch (RuntimeException e) {
            // Log the error for debugging
            System.err.println("Error incrementing views for post " + id + ": " + e.getMessage());
            // For view increments, we don't want to break the UI if it fails
            // Return 200 OK to prevent frontend errors - views are not critical
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            // Handle database transaction errors gracefully
            System.err.println("Database error incrementing views for post " + id + ": " + e.getMessage());
            e.printStackTrace();
            // Return 200 OK even on error to prevent frontend issues
            // Views are not critical, so we don't want to break the UI
            return ResponseEntity.ok().build();
        }
    }

    @PostMapping("/{id}/pin")
    public ResponseEntity<?> togglePin(@PathVariable Long id, Authentication authentication) {
        try {
            Long userId = getUserIdFromAuthentication(authentication);
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new ErrorResponse("Authentication required"));
            }

            if (!isAdmin(userId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(new ErrorResponse("Admin access required"));
            }

            postService.togglePin(id);
            return ResponseEntity.ok(new MessageResponse("Post pin status updated"));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse(e.getMessage()));
        }
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<?> getUserPosts(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {
        try {
            Long currentUserId = getUserIdFromAuthentication(authentication);
            Pageable pageable = PageRequest.of(page, size);
            Page<PostResponse> posts = postService.getUserPosts(userId, pageable, currentUserId);
            return ResponseEntity.ok(posts);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponse(e.getMessage()));
        }
    }

    private Long getUserIdFromAuthentication(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        String email = userDetails.getUsername();

        // Get user ID from database
        return userRepository.findByEmail(email)
                .map(User::getId)
                .orElse(null);
    }

    private boolean isAdmin(Long userId) {
        return userRepository.findById(userId)
                .map(user -> "ADMIN".equals(user.getRule()))
                .orElse(false);
    }

    // Helper classes
    @lombok.Data
    @lombok.AllArgsConstructor
    private static class ErrorResponse {
        private String message;
    }

    @lombok.Data
    @lombok.AllArgsConstructor
    private static class MessageResponse {
        private String message;
    }

    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    private static class RejectRequest {
        private String reason;
    }
}
