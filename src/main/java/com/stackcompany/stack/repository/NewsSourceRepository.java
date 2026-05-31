package com.stackcompany.stack.repository;

import com.stackcompany.stack.entity.NewsSource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface NewsSourceRepository extends JpaRepository<NewsSource, Long> {
    List<NewsSource> findByEnabledTrueOrderByIdAsc();
    boolean existsByListUrl(String listUrl);
}
