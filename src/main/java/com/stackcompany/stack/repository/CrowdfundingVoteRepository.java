package com.stackcompany.stack.repository;

import com.stackcompany.stack.entity.CrowdfundingVote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CrowdfundingVoteRepository extends JpaRepository<CrowdfundingVote, Long> {
    Optional<CrowdfundingVote> findByCompanyIdAndUserId(Long companyId, Long userId);
    
    boolean existsByCompanyIdAndUserId(Long companyId, Long userId);
}

