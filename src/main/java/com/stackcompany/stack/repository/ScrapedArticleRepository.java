package com.stackcompany.stack.repository;

import com.stackcompany.stack.entity.ScrapedArticle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ScrapedArticleRepository extends JpaRepository<ScrapedArticle, Long> {
    boolean existsByUrlHash(String urlHash);
    boolean existsBySourceUrl(String sourceUrl);
    boolean existsByTitleHash(String titleHash);
}
