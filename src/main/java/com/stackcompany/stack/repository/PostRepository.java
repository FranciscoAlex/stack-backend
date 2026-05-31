package com.stackcompany.stack.repository;

import com.stackcompany.stack.entity.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PostRepository extends JpaRepository<Post, Long> {
    Page<Post> findAllByOrderByCreatedAtDesc(Pageable pageable);

    @Query("SELECT p FROM Post p WHERE p.status = :status ORDER BY p.pinned DESC, p.createdAt DESC")
    Page<Post> findAllByStatusOrderByPinnedDescCreatedAtDesc(@Param("status") String status, Pageable pageable);

    @Query("SELECT p FROM Post p WHERE p.status IN :statuses ORDER BY p.pinned DESC, p.createdAt DESC")
    Page<Post> findAllByStatusInOrderByPinnedDescCreatedAtDesc(@Param("statuses") List<String> statuses,
            Pageable pageable);

    Page<Post> findAllByOrderByVotesDesc(Pageable pageable);

    @Query("SELECT p FROM Post p WHERE p.status = :status ORDER BY p.pinned DESC, p.votes DESC")
    Page<Post> findAllByStatusOrderByPinnedDescVotesDesc(@Param("status") String status, Pageable pageable);

    @Query("SELECT p FROM Post p WHERE p.status IN :statuses ORDER BY p.pinned DESC, p.votes DESC")
    Page<Post> findAllByStatusInOrderByPinnedDescVotesDesc(@Param("statuses") List<String> statuses, Pageable pageable);

    Page<Post> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    // Legacy methods for backward compatibility
    Page<Post> findAllByStatusOrderByCreatedAtDesc(String status, Pageable pageable);

    Page<Post> findAllByStatusInOrderByCreatedAtDesc(List<String> statuses, Pageable pageable);

    Page<Post> findAllByStatusOrderByVotesDesc(String status, Pageable pageable);

    Page<Post> findAllByStatusInOrderByVotesDesc(List<String> statuses, Pageable pageable);

    // Statistics queries
    long countByStatus(String status);

    @Query("SELECT COALESCE(SUM(p.views), 0) FROM Post p WHERE p.status = 'APPROVED'")
    Long sumTotalViews();

    @Query("SELECT COALESCE(SUM(p.votes), 0) FROM Post p WHERE p.status = 'APPROVED'")
    Long sumTotalVotes();

    @Query("SELECT COALESCE(SUM(p.commentsCount), 0) FROM Post p WHERE p.status = 'APPROVED'")
    Long sumTotalComments();

    @Query("SELECT COALESCE(SUM(p.sharesCount), 0) FROM Post p WHERE p.status = 'APPROVED'")
    Long sumTotalShares();

    @Query("SELECT COUNT(p) FROM Post p WHERE p.createdAt >= :since AND p.status = 'APPROVED'")
    long countApprovedPostsSince(@Param("since") LocalDateTime since);

    @Query("SELECT COUNT(p) FROM Post p WHERE p.createdAt >= :since")
    long countPostsSince(@Param("since") LocalDateTime since);

    // Most viewed post
    @Query("SELECT p FROM Post p WHERE p.status = 'APPROVED' ORDER BY p.views DESC")
    Page<Post> findMostViewedPosts(Pageable pageable);

    // Most liked post
    @Query("SELECT p FROM Post p WHERE p.status = 'APPROVED' ORDER BY p.votes DESC")
    Page<Post> findMostLikedPosts(Pageable pageable);

    // Most commented post
    @Query("SELECT p FROM Post p WHERE p.status = 'APPROVED' ORDER BY p.commentsCount DESC")
    Page<Post> findMostCommentedPosts(Pageable pageable);

    // Average engagement metrics
    @Query("SELECT AVG(p.views) FROM Post p WHERE p.status = 'APPROVED'")
    Double averageViews();

    @Query("SELECT AVG(p.votes) FROM Post p WHERE p.status = 'APPROVED'")
    Double averageVotes();

    @Query("SELECT AVG(p.commentsCount) FROM Post p WHERE p.status = 'APPROVED'")
    Double averageComments();

    // Filtered feeds
    @Query("SELECT p FROM Post p JOIN p.tags t WHERE t.name = :tagName AND p.status = 'APPROVED' ORDER BY p.createdAt DESC")
    Page<Post> findByTagName(@Param("tagName") String tagName, Pageable pageable);

    @Query("SELECT p FROM Post p WHERE p.user.id IN :userIds AND p.status = 'APPROVED' ORDER BY p.createdAt DESC")
    Page<Post> findByUserIdIn(@Param("userIds") List<Long> userIds, Pageable pageable);

    // Bot duplicate check
    @Query("SELECT p FROM Post p WHERE LOWER(p.content) LIKE LOWER(CONCAT('%', :term, '%')) AND p.createdAt >= :since")
    List<Post> findRecentByContentContaining(@Param("term") String term, @Param("since") LocalDateTime since);
}
