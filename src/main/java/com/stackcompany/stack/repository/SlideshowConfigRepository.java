package com.stackcompany.stack.repository;

import com.stackcompany.stack.entity.SlideshowConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SlideshowConfigRepository extends JpaRepository<SlideshowConfig, Long> {
    List<SlideshowConfig> findByPageKeyAndEnabledTrueOrderByDisplayOrderAsc(String pageKey);
    List<SlideshowConfig> findByPageKeyOrderByDisplayOrderAsc(String pageKey);
}

