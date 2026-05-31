package com.stackcompany.stack.service;

import com.stackcompany.stack.dto.PostRequest;
import com.stackcompany.stack.dto.PostResponse;
import com.stackcompany.stack.entity.Post;
import com.stackcompany.stack.entity.PostVote;
import com.stackcompany.stack.entity.User;
import com.stackcompany.stack.repository.PostRepository;
import com.stackcompany.stack.repository.PostVoteRepository;
import com.stackcompany.stack.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class PostService {

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PostVoteRepository postVoteRepository;

    @Autowired
    private com.stackcompany.stack.repository.PostBookmarkRepository postBookmarkRepository;

    @Autowired
    private com.stackcompany.stack.repository.TagRepository tagRepository;

    @Autowired
    private com.stackcompany.stack.repository.UserFollowRepository userFollowRepository;

    @Autowired
    private EmailService emailService;

    @Transactional
    public PostResponse createPost(Long userId, PostRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Set status based on user role: ADMIN posts are auto-approved, USER posts need
        // approval
        String status = "ADMIN".equals(user.getRule()) ? "APPROVED" : "PENDING";

        Post post = Post.builder()
                .user(user)
                .content(request.getContent())
                .votes(0)
                .commentsCount(0)
                .sharesCount(0)
                .views(0)
                .status(status)
                .build();

        // Handle Tags
        if (request.getTags() != null && !request.getTags().isEmpty()) {
            List<com.stackcompany.stack.entity.Tag> postTags = new java.util.ArrayList<>();
            for (String tagName : request.getTags()) {
                if (tagName != null && !tagName.trim().isEmpty()) {
                    String cleanName = tagName.trim();
                    com.stackcompany.stack.entity.Tag tag = tagRepository.findByName(cleanName)
                            .orElseGet(() -> tagRepository.save(
                                    com.stackcompany.stack.entity.Tag.builder().name(cleanName).build()));
                    postTags.add(tag);
                }
            }
            post.setTags(postTags);
        }

        // Handle image URLs - support both new imageUrls list and legacy imageUrl
        if (request.getImageUrls() != null && !request.getImageUrls().isEmpty()) {
            // Limit to 5 images
            List<String> imageUrls = request.getImageUrls().stream()
                    .limit(5)
                    .filter(url -> url != null && !url.trim().isEmpty())
                    .collect(java.util.stream.Collectors.toList());
            if (!imageUrls.isEmpty()) {
                post.setImageUrls(imageUrls);
            }
        } else if (request.getImageUrl() != null && !request.getImageUrl().trim().isEmpty()) {
            // Backward compatibility: convert single imageUrl to list
            post.setImageUrl(request.getImageUrl());
        }

        post = postRepository.save(post);
        postRepository.flush();
        // Ensure imageUrls are loaded by calling getImageUrls
        post.getImageUrls(); // This will parse the JSON if needed
        return convertToResponse(post, userId);
    }

    public Page<PostResponse> getAllPosts(Pageable pageable, Long currentUserId) {
        Page<Post> posts;

        if (currentUserId == null) {
            // Unauthenticated users only see APPROVED posts, pinned first
            posts = postRepository.findAllByStatusOrderByPinnedDescCreatedAtDesc("APPROVED", pageable);
        } else {
            // Check if current user is admin
            User currentUser = userRepository.findById(currentUserId).orElse(null);
            boolean isAdmin = currentUser != null && "ADMIN".equals(currentUser.getRule());

            if (isAdmin) {
                // Admins only see APPROVED posts in main feed (PENDING posts are in admin
                // feed), pinned first
                posts = postRepository.findAllByStatusOrderByPinnedDescCreatedAtDesc("APPROVED", pageable);
            } else {
                // Authenticated normal users see both APPROVED and PENDING posts, pinned first
                posts = postRepository.findAllByStatusInOrderByPinnedDescCreatedAtDesc(
                        java.util.Arrays.asList("APPROVED", "PENDING"), pageable);
            }
        }
        return posts.map(post -> convertToResponse(post, currentUserId));
    }

    public Page<PostResponse> getPopularPosts(Pageable pageable, Long currentUserId) {
        Page<Post> posts;

        if (currentUserId == null) {
            // Unauthenticated users only see APPROVED posts, pinned first
            posts = postRepository.findAllByStatusOrderByPinnedDescVotesDesc("APPROVED", pageable);
        } else {
            // Check if current user is admin
            User currentUser = userRepository.findById(currentUserId).orElse(null);
            boolean isAdmin = currentUser != null && "ADMIN".equals(currentUser.getRule());

            if (isAdmin) {
                // Admins only see APPROVED posts in main feed, pinned first
                posts = postRepository.findAllByStatusOrderByPinnedDescVotesDesc("APPROVED", pageable);
            } else {
                // Authenticated normal users see both APPROVED and PENDING posts, pinned first
                posts = postRepository.findAllByStatusInOrderByPinnedDescVotesDesc(
                        java.util.Arrays.asList("APPROVED", "PENDING"), pageable);
            }
        }
        return posts.map(post -> convertToResponse(post, currentUserId));
    }

    public Page<PostResponse> getPendingPosts(Pageable pageable, Long currentUserId) {
        // Get all pending posts for admin review
        Page<Post> posts = postRepository.findAllByStatusOrderByCreatedAtDesc("PENDING", pageable);
        return posts.map(post -> convertToResponse(post, currentUserId));
    }

    public Page<PostResponse> getPostsByTag(String tagName, Pageable pageable, Long currentUserId) {
        Page<Post> posts = postRepository.findByTagName(tagName, pageable);
        return posts.map(post -> convertToResponse(post, currentUserId));
    }

    public Page<PostResponse> getFollowingPosts(Pageable pageable, Long currentUserId) {
        if (currentUserId == null) {
            return Page.empty(pageable);
        }

        List<com.stackcompany.stack.entity.UserFollow> follows = userFollowRepository.findByFollowerId(currentUserId);
        List<Long> followingIds = follows.stream()
                .map(follow -> follow.getFollowing().getId())
                .collect(java.util.stream.Collectors.toList());

        if (followingIds.isEmpty()) {
            return Page.empty(pageable);
        }

        Page<Post> posts = postRepository.findByUserIdIn(followingIds, pageable);
        return posts.map(post -> convertToResponse(post, currentUserId));
    }

    public Page<PostResponse> getUserPosts(Long userId, Pageable pageable, Long currentUserId) {
        // Get all posts by the specified user
        Page<Post> posts = postRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);

        // Filter posts based on current user's permissions
        if (currentUserId == null) {
            // Unauthenticated users only see APPROVED posts
            // Filter and convert to response
            List<Post> filteredPosts = posts.getContent().stream()
                    .filter(post -> "APPROVED".equals(post.getStatus()))
                    .collect(java.util.stream.Collectors.toList());

            // Create a new page with filtered content
            Page<Post> filteredPage = new PageImpl<>(
                    filteredPosts, pageable, filteredPosts.size());
            return filteredPage.map(post -> convertToResponse(post, currentUserId));
        } else {
            // Check if current user is admin
            User currentUser = userRepository.findById(currentUserId).orElse(null);
            boolean isAdmin = currentUser != null && "ADMIN".equals(currentUser.getRule());

            if (!isAdmin) {
                // Authenticated normal users can see APPROVED and PENDING posts
                List<Post> filteredPosts = posts.getContent().stream()
                        .filter(post -> "APPROVED".equals(post.getStatus()) || "PENDING".equals(post.getStatus()))
                        .collect(java.util.stream.Collectors.toList());

                // Create a new page with filtered content
                Page<Post> filteredPage = new org.springframework.data.domain.PageImpl<>(
                        filteredPosts, pageable, filteredPosts.size());
                return filteredPage.map(post -> convertToResponse(post, currentUserId));
            }
            // Admins can see all posts (no filtering needed)
        }

        return posts.map(post -> convertToResponse(post, currentUserId));
    }

    public PostResponse getPostById(Long postId, Long currentUserId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post not found"));

        // Unauthenticated users can only see APPROVED posts
        if (currentUserId == null) {
            if (!"APPROVED".equals(post.getStatus())) {
                throw new RuntimeException("Post not found");
            }
            return convertToResponse(post, currentUserId);
        }

        // Check if user is admin
        User currentUser = userRepository.findById(currentUserId).orElse(null);
        boolean isAdmin = currentUser != null && "ADMIN".equals(currentUser.getRule());

        // Admins can see all posts, authenticated normal users can see APPROVED and
        // PENDING posts
        if (!isAdmin && !"APPROVED".equals(post.getStatus()) && !"PENDING".equals(post.getStatus())) {
            throw new RuntimeException("Post not found");
        }

        return convertToResponse(post, currentUserId);
    }

    @Transactional
    public PostResponse updatePost(Long postId, Long userId, PostRequest request) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post not found"));

        if (!post.getUser().getId().equals(userId)) {
            throw new RuntimeException("You don't have permission to update this post");
        }

        post.setContent(request.getContent());

        // Handle image URLs - support both new imageUrls list and legacy imageUrl
        if (request.getImageUrls() != null && !request.getImageUrls().isEmpty()) {
            // Limit to 5 images
            List<String> imageUrls = request.getImageUrls().stream()
                    .limit(5)
                    .filter(url -> url != null && !url.trim().isEmpty())
                    .collect(java.util.stream.Collectors.toList());
            post.setImageUrls(imageUrls);
        } else if (request.getImageUrl() != null && !request.getImageUrl().trim().isEmpty()) {
            // Backward compatibility: convert single imageUrl to list
            post.setImageUrl(request.getImageUrl());
        } else {
            // Clear images if both are null/empty
            post.setImageUrls(new java.util.ArrayList<>());
        }

        post = postRepository.save(post);

        return convertToResponse(post, userId);
    }

    @Transactional
    public void deletePost(Long postId, Long userId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post not found"));

        if (!post.getUser().getId().equals(userId)) {
            throw new RuntimeException("You don't have permission to delete this post");
        }

        postRepository.delete(post);
    }

    @Transactional
    public void votePost(Long postId, Long userId, String voteType) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post not found"));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        PostVote existingVote = postVoteRepository.findByPostIdAndUserId(postId, userId)
                .orElse(null);

        if ("remove".equals(voteType)) {
            if (existingVote != null) {
                if ("up".equals(existingVote.getVoteType())) {
                    post.setVotes(post.getVotes() - 1);
                } else {
                    post.setVotes(post.getVotes() + 1);
                }
                postVoteRepository.delete(existingVote);
                postRepository.save(post);
            }
        } else {
            if (existingVote != null) {
                String oldVoteType = existingVote.getVoteType();
                if (oldVoteType.equals(voteType)) {
                    // Same vote, remove it
                    if ("up".equals(oldVoteType)) {
                        post.setVotes(post.getVotes() - 1);
                    } else {
                        post.setVotes(post.getVotes() + 1);
                    }
                    postVoteRepository.delete(existingVote);
                } else {
                    // Different vote, update it
                    if ("up".equals(oldVoteType)) {
                        post.setVotes(post.getVotes() - 2); // Remove upvote, add downvote
                    } else {
                        post.setVotes(post.getVotes() + 2); // Remove downvote, add upvote
                    }
                    existingVote.setVoteType(voteType);
                    postVoteRepository.save(existingVote);
                }
            } else {
                // New vote
                PostVote newVote = PostVote.builder()
                        .post(post)
                        .user(user)
                        .voteType(voteType)
                        .build();
                postVoteRepository.save(newVote);

                if ("up".equals(voteType)) {
                    post.setVotes(post.getVotes() + 1);
                } else {
                    post.setVotes(post.getVotes() - 1);
                }
            }
            postRepository.save(post);
        }
    }

    private PostResponse convertToResponse(Post post, Long currentUserId) {
        final String[] userVote = { null };
        final Boolean[] isBookmarked = { false };

        if (currentUserId != null) {
            postVoteRepository.findByPostIdAndUserId(post.getId(), currentUserId)
                    .ifPresent(vote -> userVote[0] = vote.getVoteType());
            isBookmarked[0] = postBookmarkRepository.existsByPostIdAndUserId(post.getId(), currentUserId);
        }

        PostResponse.AuthorInfo authorInfo = PostResponse.AuthorInfo.builder()
                .id(post.getUser().getId())
                .email(post.getUser().getEmail())
                .firstName(post.getUser().getFirstName())
                .lastName(post.getUser().getLastName())
                .avatar(post.getUser().getAvatarUrl())
                .build();

        List<String> imageUrls = post.getImageUrls();
        List<String> tags = post.getTags().stream()
                .map(com.stackcompany.stack.entity.Tag::getName)
                .collect(java.util.stream.Collectors.toList());

        return PostResponse.builder()
                .id(post.getId())
                .content(post.getContent())
                .imageUrl(imageUrls.isEmpty() ? null : imageUrls.get(0)) // Backward compatibility
                .imageUrls(imageUrls) // New field with all images
                .tags(tags)
                .votes(post.getVotes())
                .commentsCount(post.getCommentsCount())
                .sharesCount(post.getSharesCount())
                .views(post.getViews())
                .createdAt(post.getCreatedAt())
                .updatedAt(post.getUpdatedAt())
                .author(authorInfo)
                .userVote(userVote[0])
                .isBookmarked(isBookmarked[0])
                .pinned(post.getPinned())
                .build();
    }

    @Transactional
    public void toggleBookmark(Long postId, Long userId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post not found"));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Optional<com.stackcompany.stack.entity.PostBookmark> existingBookmark = postBookmarkRepository
                .findByPostIdAndUserId(postId, userId);

        if (existingBookmark.isPresent()) {
            // Remove bookmark
            postBookmarkRepository.delete(existingBookmark.get());
        } else {
            // Add bookmark
            com.stackcompany.stack.entity.PostBookmark bookmark = com.stackcompany.stack.entity.PostBookmark.builder()
                    .post(post)
                    .user(user)
                    .build();
            postBookmarkRepository.save(bookmark);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void incrementViews(Long postId) {
        try {
            Post post = postRepository.findById(postId)
                    .orElseThrow(() -> new RuntimeException("Post not found"));
            post.setViews(post.getViews() + 1);
            postRepository.save(post);
            postRepository.flush(); // Ensure immediate persistence
        } catch (Exception e) {
            // Log the error and rethrow to trigger rollback
            System.err.println("Error incrementing views for post " + postId + ": " + e.getMessage());
            throw e;
        }
    }

    @Transactional
    public void togglePin(Long postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post not found"));

        post.setPinned(!post.getPinned());
        postRepository.save(post);
    }

    @Transactional
    public void approvePost(Long postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post not found"));

        if (!"PENDING".equals(post.getStatus())) {
            throw new RuntimeException("Only pending posts can be approved");
        }

        post.setStatus("APPROVED");
        postRepository.save(post);
    }

    @Transactional
    public void rejectPost(Long postId, String reason) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post not found"));

        if (!"PENDING".equals(post.getStatus())) {
            throw new RuntimeException("Only pending posts can be rejected");
        }

        post.setStatus("REJECTED");
        postRepository.save(post);

        // Send rejection email to post owner
        User postOwner = post.getUser();
        String firstName = postOwner.getFirstName() != null ? postOwner.getFirstName() : "User";
        emailService.sendPostRejectionEmail(postOwner.getEmail(), firstName, post.getContent(), reason);
    }
}
