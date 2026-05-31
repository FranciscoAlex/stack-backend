package com.stackcompany.stack.repository;

import com.stackcompany.stack.entity.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {
    
    // Get top-level comments for a post (parent_id is null)
    Page<Comment> findByPostIdAndParentIdIsNullOrderByCreatedAtDesc(Long postId, Pageable pageable);
    
    // Get replies to a specific comment
    List<Comment> findByParentIdOrderByCreatedAtAsc(Long parentId);
    
    // Count comments for a post
    long countByPostId(Long postId);
    
    // Count replies for a comment
    long countByParentId(Long parentId);
    
    // Get all comments for a post (including nested)
    @Query("SELECT c FROM Comment c WHERE c.post.id = :postId ORDER BY c.createdAt ASC")
    List<Comment> findAllByPostId(@Param("postId") Long postId);
}

