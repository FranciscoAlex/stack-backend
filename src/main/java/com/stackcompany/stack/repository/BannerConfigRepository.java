package com.stackcompany.stack.repository;

import com.stackcompany.stack.entity.BannerConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BannerConfigRepository extends JpaRepository<BannerConfig, Long> {
    Optional<BannerConfig> findByPageKey(String pageKey);
}

