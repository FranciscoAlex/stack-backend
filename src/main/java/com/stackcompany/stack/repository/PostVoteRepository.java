package com.stackcompany.stack.repository;

import com.stackcompany.stack.entity.PostVote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PostVoteRepository extends JpaRepository<PostVote, Long> {
    Optional<PostVote> findByPostIdAndUserId(Long postId, Long userId);
    
    @Query("SELECT COUNT(pv) FROM PostVote pv WHERE pv.post.id = :postId AND pv.voteType = 'up'")
    Long countUpvotesByPostId(@Param("postId") Long postId);
    
    @Query("SELECT COUNT(pv) FROM PostVote pv WHERE pv.post.id = :postId AND pv.voteType = 'down'")
    Long countDownvotesByPostId(@Param("postId") Long postId);
    
    void deleteByPostIdAndUserId(Long postId, Long userId);
}

