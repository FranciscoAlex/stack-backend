package com.stackcompany.stack.repository;

import com.stackcompany.stack.entity.CrowdfundingCompany;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CrowdfundingCompanyRepository extends JpaRepository<CrowdfundingCompany, Long> {
    Page<CrowdfundingCompany> findAllByOrderByCreatedAtDesc(Pageable pageable);
    
    Page<CrowdfundingCompany> findAllByOrderByVotesDesc(Pageable pageable);
}

