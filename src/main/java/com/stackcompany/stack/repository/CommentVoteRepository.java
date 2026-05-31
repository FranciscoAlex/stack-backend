package com.stackcompany.stack.repository;

import com.stackcompany.stack.entity.CommentVote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CommentVoteRepository extends JpaRepository<CommentVote, Long> {
    Optional<CommentVote> findByCommentIdAndUserId(Long commentId, Long userId);
}

