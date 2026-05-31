package com.stackcompany.stack.service;

import com.stackcompany.stack.dto.CommentRequest;
import com.stackcompany.stack.dto.CommentResponse;
import com.stackcompany.stack.entity.Comment;
import com.stackcompany.stack.entity.CommentVote;
import com.stackcompany.stack.entity.Post;
import com.stackcompany.stack.entity.User;
import com.stackcompany.stack.repository.CommentRepository;
import com.stackcompany.stack.repository.CommentVoteRepository;
import com.stackcompany.stack.repository.PostRepository;
import com.stackcompany.stack.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class CommentService {
    
    @Autowired
    private CommentRepository commentRepository;
    
    @Autowired
    private PostRepository postRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private CommentVoteRepository commentVoteRepository;
    
    @Transactional
    public CommentResponse createComment(Long postId, Long userId, CommentRequest request) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post not found"));
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        // Validate parent comment if provided
        if (request.getParentId() != null) {
            Comment parentComment = commentRepository.findById(request.getParentId())
                    .orElseThrow(() -> new RuntimeException("Parent comment not found"));
            
            // Ensure parent comment belongs to the same post
            if (!parentComment.getPost().getId().equals(postId)) {
                throw new RuntimeException("Parent comment does not belong to this post");
            }
        }
        
        Comment comment = Comment.builder()
                .post(post)
                .user(user)
                .parentId(request.getParentId())
                .content(request.getContent())
                .votes(0)
                .repliesCount(0)
                .build();
        
        comment = commentRepository.save(comment);
        
        // Update post comments count
        post.setCommentsCount(post.getCommentsCount() + 1);
        postRepository.save(post);
        
        // Update parent comment replies count if this is a reply
        if (request.getParentId() != null) {
            Comment parentComment = commentRepository.findById(request.getParentId())
                    .orElseThrow(() -> new RuntimeException("Parent comment not found"));
            parentComment.setRepliesCount(parentComment.getRepliesCount() + 1);
            commentRepository.save(parentComment);
        }
        
        return convertToResponse(comment, userId);
    }
    
    public Page<CommentResponse> getCommentsByPostId(Long postId, Pageable pageable, Long currentUserId) {
        Page<Comment> comments = commentRepository.findByPostIdAndParentIdIsNullOrderByCreatedAtDesc(postId, pageable);
        return comments.map(comment -> convertToResponseWithReplies(comment, currentUserId));
    }
    
    public List<CommentResponse> getAllCommentsByPostId(Long postId, Long currentUserId) {
        List<Comment> allComments = commentRepository.findAllByPostId(postId);
        
        // Build a map of comments by ID
        Map<Long, CommentResponse> commentMap = allComments.stream()
                .map(comment -> convertToResponse(comment, currentUserId))
                .collect(Collectors.toMap(CommentResponse::getId, c -> c));
        
        // Build the tree structure
        List<CommentResponse> topLevelComments = new ArrayList<>();
        for (Comment comment : allComments) {
            CommentResponse response = commentMap.get(comment.getId());
            if (comment.getParentId() == null) {
                // Top-level comment
                topLevelComments.add(response);
            } else {
                // Reply - add to parent's replies list
                CommentResponse parent = commentMap.get(comment.getParentId());
                if (parent != null) {
                    if (parent.getReplies() == null) {
                        parent.setReplies(new ArrayList<>());
                    }
                    parent.getReplies().add(response);
                }
            }
        }
        
        // Sort replies by creation date
        for (CommentResponse comment : commentMap.values()) {
            if (comment.getReplies() != null && !comment.getReplies().isEmpty()) {
                comment.getReplies().sort((a, b) -> a.getCreatedAt().compareTo(b.getCreatedAt()));
            }
        }
        
        return topLevelComments;
    }
    
    @Transactional
    public CommentResponse updateComment(Long commentId, Long userId, CommentRequest request) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new RuntimeException("Comment not found"));
        
        if (!comment.getUser().getId().equals(userId)) {
            throw new RuntimeException("You don't have permission to update this comment");
        }
        
        comment.setContent(request.getContent());
        comment = commentRepository.save(comment);
        
        return convertToResponse(comment, userId);
    }
    
    @Transactional
    public void deleteComment(Long commentId, Long userId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new RuntimeException("Comment not found"));
        
        if (!comment.getUser().getId().equals(userId)) {
            throw new RuntimeException("You don't have permission to delete this comment");
        }
        
        Post post = comment.getPost();
        
        // If this is a top-level comment, decrease post comments count
        if (comment.getParentId() == null) {
            post.setCommentsCount(Math.max(0, post.getCommentsCount() - 1));
        } else {
            // If this is a reply, decrease parent comment replies count
            Comment parentComment = commentRepository.findById(comment.getParentId())
                    .orElseThrow(() -> new RuntimeException("Parent comment not found"));
            parentComment.setRepliesCount(Math.max(0, parentComment.getRepliesCount() - 1));
            commentRepository.save(parentComment);
        }
        
        // Delete all replies to this comment (cascade delete)
        List<Comment> replies = commentRepository.findByParentIdOrderByCreatedAtAsc(commentId);
        for (Comment reply : replies) {
            commentRepository.delete(reply);
        }
        
        commentRepository.delete(comment);
        postRepository.save(post);
    }
    
    @Transactional
    public void voteComment(Long commentId, Long userId, String voteType) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new RuntimeException("Comment not found"));
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        Optional<CommentVote> existingVote = commentVoteRepository.findByCommentIdAndUserId(commentId, userId);
        
        if ("remove".equals(voteType)) {
            if (existingVote.isPresent()) {
                CommentVote vote = existingVote.get();
                if ("up".equals(vote.getVoteType())) {
                    comment.setVotes(comment.getVotes() - 1);
                } else {
                    comment.setVotes(comment.getVotes() + 1);
                }
                commentVoteRepository.delete(vote);
                commentRepository.save(comment);
            }
        } else {
            if (existingVote.isPresent()) {
                CommentVote vote = existingVote.get();
                String oldVoteType = vote.getVoteType();
                if (oldVoteType.equals(voteType)) {
                    // Same vote, remove it
                    if ("up".equals(oldVoteType)) {
                        comment.setVotes(comment.getVotes() - 1);
                    } else {
                        comment.setVotes(comment.getVotes() + 1);
                    }
                    commentVoteRepository.delete(vote);
                } else {
                    // Different vote, update it
                    if ("up".equals(oldVoteType)) {
                        comment.setVotes(comment.getVotes() - 2); // Remove upvote, add downvote
                    } else {
                        comment.setVotes(comment.getVotes() + 2); // Remove downvote, add upvote
                    }
                    vote.setVoteType(voteType);
                    commentVoteRepository.save(vote);
                }
            } else {
                // New vote
                CommentVote newVote = CommentVote.builder()
                        .comment(comment)
                        .user(user)
                        .voteType(voteType)
                        .build();
                commentVoteRepository.save(newVote);
                
                if ("up".equals(voteType)) {
                    comment.setVotes(comment.getVotes() + 1);
                } else {
                    comment.setVotes(comment.getVotes() - 1);
                }
            }
            commentRepository.save(comment);
        }
    }
    
    private CommentResponse convertToResponse(Comment comment, Long currentUserId) {
        final String[] userVote = {null};
        if (currentUserId != null) {
            commentVoteRepository.findByCommentIdAndUserId(comment.getId(), currentUserId)
                    .ifPresent(vote -> userVote[0] = vote.getVoteType());
        }
        
        CommentResponse.AuthorInfo authorInfo = CommentResponse.AuthorInfo.builder()
                .id(comment.getUser().getId())
                .email(comment.getUser().getEmail())
                .firstName(comment.getUser().getFirstName())
                .lastName(comment.getUser().getLastName())
                .avatar(comment.getUser().getAvatarUrl())
                .build();
        
        return CommentResponse.builder()
                .id(comment.getId())
                .postId(comment.getPost().getId())
                .parentId(comment.getParentId())
                .content(comment.getContent())
                .votes(comment.getVotes())
                .repliesCount(comment.getRepliesCount())
                .createdAt(comment.getCreatedAt())
                .updatedAt(comment.getUpdatedAt())
                .author(authorInfo)
                .userVote(userVote[0])
                .replies(new ArrayList<>())
                .build();
    }
    
    private CommentResponse convertToResponseWithReplies(Comment comment, Long currentUserId) {
        CommentResponse response = convertToResponse(comment, currentUserId);
        
        // Load replies
        List<Comment> replies = commentRepository.findByParentIdOrderByCreatedAtAsc(comment.getId());
        List<CommentResponse> replyResponses = replies.stream()
                .map(reply -> convertToResponse(reply, currentUserId))
                .collect(Collectors.toList());
        
        response.setReplies(replyResponses);
        return response;
    }
}

