package com.stackcompany.stack.controller;

import com.stackcompany.stack.dto.CommentRequest;
import com.stackcompany.stack.dto.CommentResponse;
import com.stackcompany.stack.dto.VoteRequest;
import com.stackcompany.stack.entity.User;
import com.stackcompany.stack.repository.UserRepository;
import com.stackcompany.stack.service.CommentService;
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

import java.util.List;

@RestController
@RequestMapping("/api/comments")
@CrossOrigin(origins = "*")
public class CommentController {
    
    @Autowired
    private CommentService commentService;
    
    @Autowired
    private UserRepository userRepository;
    
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
    
    @PostMapping("/post/{postId}")
    public ResponseEntity<?> createComment(
            @PathVariable Long postId,
            @Valid @RequestBody CommentRequest request,
            Authentication authentication) {
        try {
            Long userId = getUserIdFromAuthentication(authentication);
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new ErrorResponse("Authentication required"));
            }
            CommentResponse response = commentService.createComment(postId, userId, request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse(e.getMessage()));
        }
    }
    
    @GetMapping("/post/{postId}")
    public ResponseEntity<List<CommentResponse>> getCommentsByPostId(
            @PathVariable Long postId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            Authentication authentication) {
        Long userId = getUserIdFromAuthentication(authentication);
        Pageable pageable = PageRequest.of(page, size);
        Page<CommentResponse> comments = commentService.getCommentsByPostId(postId, pageable, userId);
        return ResponseEntity.ok(comments.getContent());
    }
    
    @GetMapping("/post/{postId}/all")
    public ResponseEntity<List<CommentResponse>> getAllCommentsByPostId(
            @PathVariable Long postId,
            Authentication authentication) {
        Long userId = getUserIdFromAuthentication(authentication);
        List<CommentResponse> comments = commentService.getAllCommentsByPostId(postId, userId);
        return ResponseEntity.ok(comments);
    }
    
    @PutMapping("/{commentId}")
    public ResponseEntity<?> updateComment(
            @PathVariable Long commentId,
            @Valid @RequestBody CommentRequest request,
            Authentication authentication) {
        try {
            Long userId = getUserIdFromAuthentication(authentication);
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new ErrorResponse("Authentication required"));
            }
            CommentResponse response = commentService.updateComment(commentId, userId, request);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse(e.getMessage()));
        }
    }
    
    @DeleteMapping("/{commentId}")
    public ResponseEntity<?> deleteComment(
            @PathVariable Long commentId,
            Authentication authentication) {
        try {
            Long userId = getUserIdFromAuthentication(authentication);
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new ErrorResponse("Authentication required"));
            }
            commentService.deleteComment(commentId, userId);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse(e.getMessage()));
        }
    }
    
    @PostMapping("/{commentId}/vote")
    public ResponseEntity<?> voteComment(
            @PathVariable Long commentId,
            @RequestBody VoteRequest request,
            Authentication authentication) {
        try {
            Long userId = getUserIdFromAuthentication(authentication);
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new ErrorResponse("Authentication required"));
            }
            commentService.voteComment(commentId, userId, request.getVoteType());
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse(e.getMessage()));
        }
    }
    
    // Simple error response class
    private static class ErrorResponse {
        private String message;
        
        public ErrorResponse(String message) {
            this.message = message;
        }
        
        public String getMessage() {
            return message;
        }
    }
}

