package com.stackcompany.stack.service;

import com.stackcompany.stack.dto.CrowdfundingCompanyRequest;
import com.stackcompany.stack.dto.CrowdfundingCompanyResponse;
import com.stackcompany.stack.entity.CrowdfundingCompany;
import com.stackcompany.stack.entity.CrowdfundingVote;
import com.stackcompany.stack.entity.User;
import com.stackcompany.stack.repository.CrowdfundingCompanyRepository;
import com.stackcompany.stack.repository.CrowdfundingVoteRepository;
import com.stackcompany.stack.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CrowdfundingService {
    
    @Autowired
    private CrowdfundingCompanyRepository companyRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private CrowdfundingVoteRepository voteRepository;
    
    @Transactional
    public CrowdfundingCompanyResponse createCompany(Long userId, CrowdfundingCompanyRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        CrowdfundingCompany company = CrowdfundingCompany.builder()
                .user(user)
                .title(request.getTitle())
                .subtitle(request.getSubtitle())
                .description(request.getDescription())
                .goalAmount(request.getGoalAmount())
                .raisedAmount(java.math.BigDecimal.ZERO)
                .iconUrl(request.getIconUrl())
                .imageUrls(request.getImageUrls())
                .productUrl(request.getProductUrl())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .votes(0)
                .commentsCount(0)
                .tags(request.getTags())
                .build();
        
        company = companyRepository.save(company);
        return convertToResponse(company, userId);
    }
    
    public Page<CrowdfundingCompanyResponse> getAllCompanies(Pageable pageable, Long currentUserId) {
        Page<CrowdfundingCompany> companies = companyRepository.findAllByOrderByCreatedAtDesc(pageable);
        return companies.map(company -> convertToResponse(company, currentUserId));
    }
    
    public Page<CrowdfundingCompanyResponse> getTopCompanies(Pageable pageable, Long currentUserId) {
        Page<CrowdfundingCompany> companies = companyRepository.findAllByOrderByVotesDesc(pageable);
        return companies.map(company -> convertToResponse(company, currentUserId));
    }
    
    public List<CrowdfundingCompanyResponse> getTop3Companies(Long currentUserId) {
        Pageable pageable = PageRequest.of(0, 3);
        Page<CrowdfundingCompany> companiesPage = companyRepository.findAllByOrderByVotesDesc(pageable);
        List<CrowdfundingCompany> companies = companiesPage.getContent();
        return companies.stream()
                .map(company -> convertToResponse(company, currentUserId))
                .collect(Collectors.toList());
    }
    
    public CrowdfundingCompanyResponse getCompanyById(Long companyId, Long currentUserId) {
        CrowdfundingCompany company = companyRepository.findById(companyId)
                .orElseThrow(() -> new RuntimeException("Company not found"));
        return convertToResponse(company, currentUserId);
    }
    
    @Transactional
    public CrowdfundingCompanyResponse updateCompany(Long companyId, Long userId, CrowdfundingCompanyRequest request) {
        CrowdfundingCompany company = companyRepository.findById(companyId)
                .orElseThrow(() -> new RuntimeException("Company not found"));
        
        if (!company.getUser().getId().equals(userId)) {
            throw new RuntimeException("You don't have permission to update this company");
        }
        
        company.setTitle(request.getTitle());
        company.setSubtitle(request.getSubtitle());
        company.setDescription(request.getDescription());
        company.setGoalAmount(request.getGoalAmount());
        company.setIconUrl(request.getIconUrl());
        company.setImageUrls(request.getImageUrls());
        company.setProductUrl(request.getProductUrl());
        company.setStartDate(request.getStartDate());
        company.setEndDate(request.getEndDate());
        company.setTags(request.getTags());
        
        company = companyRepository.save(company);
        return convertToResponse(company, userId);
    }
    
    @Transactional
    public void deleteCompany(Long companyId, Long userId) {
        CrowdfundingCompany company = companyRepository.findById(companyId)
                .orElseThrow(() -> new RuntimeException("Company not found"));
        
        if (!company.getUser().getId().equals(userId)) {
            throw new RuntimeException("You don't have permission to delete this company");
        }
        
        companyRepository.delete(company);
    }
    
    @Transactional
    public void voteCompany(Long companyId, Long userId) {
        CrowdfundingCompany company = companyRepository.findById(companyId)
                .orElseThrow(() -> new RuntimeException("Company not found"));
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        CrowdfundingVote existingVote = voteRepository.findByCompanyIdAndUserId(companyId, userId)
                .orElse(null);
        
        if (existingVote != null) {
            // Remove vote
            voteRepository.delete(existingVote);
            // The trigger will automatically update the votes count
        } else {
            // Add vote
            CrowdfundingVote newVote = CrowdfundingVote.builder()
                    .company(company)
                    .user(user)
                    .build();
            voteRepository.save(newVote);
            // The trigger will automatically update the votes count
        }
        
        // Refresh company to get updated vote count
        companyRepository.flush();
    }
    
    private CrowdfundingCompanyResponse convertToResponse(CrowdfundingCompany company, Long currentUserId) {
        Boolean hasVoted = false;
        
        if (currentUserId != null) {
            hasVoted = voteRepository.existsByCompanyIdAndUserId(company.getId(), currentUserId);
        }
        
        CrowdfundingCompanyResponse.AuthorInfo authorInfo = CrowdfundingCompanyResponse.AuthorInfo.builder()
                .id(company.getUser().getId())
                .email(company.getUser().getEmail())
                .firstName(company.getUser().getFirstName())
                .lastName(company.getUser().getLastName())
                .avatar(company.getUser().getAvatarUrl())
                .build();
        
        return CrowdfundingCompanyResponse.builder()
                .id(company.getId())
                .title(company.getTitle())
                .subtitle(company.getSubtitle())
                .description(company.getDescription())
                .goalAmount(company.getGoalAmount())
                .raisedAmount(company.getRaisedAmount())
                .iconUrl(company.getIconUrl())
                .imageUrls(company.getImageUrls())
                .productUrl(company.getProductUrl())
                .startDate(company.getStartDate())
                .endDate(company.getEndDate())
                .votes(company.getVotes())
                .commentsCount(company.getCommentsCount())
                .tags(company.getTags())
                .createdAt(company.getCreatedAt())
                .updatedAt(company.getUpdatedAt())
                .author(authorInfo)
                .hasVoted(hasVoted)
                .build();
    }
}

