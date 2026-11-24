package com.stackcompany.stack.repository;

import com.stackcompany.stack.entity.ScrapedArticle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ScrapedArticleRepository extends JpaRepository<ScrapedArticle, Long> {
    boolean existsByUrlHash(String urlHash);
    java.util.List<ScrapedArticle> findTop200ByStatusAndPostIdIsNotNullOrderByIdDesc(String status);
    boolean existsBySourceUrl(String sourceUrl);
    boolean existsByTitleHash(String titleHash);
}
