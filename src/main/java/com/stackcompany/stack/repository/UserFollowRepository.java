package com.stackcompany.stack.repository;

import com.stackcompany.stack.entity.UserFollow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserFollowRepository extends JpaRepository<UserFollow, Long> {
    Optional<UserFollow> findByFollowerIdAndFollowingId(Long followerId, Long followingId);

    List<UserFollow> findByFollowerId(Long followerId);

    List<UserFollow> findByFollowingId(Long followingId);

    boolean existsByFollowerIdAndFollowingId(Long followerId, Long followingId);
}
