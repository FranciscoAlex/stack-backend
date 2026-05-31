package com.stackcompany.stack.repository;

import com.stackcompany.stack.entity.ShortVideo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ShortVideoRepository extends JpaRepository<ShortVideo, Long> {
    List<ShortVideo> findByPageKeyOrderByDisplayOrderAsc(String pageKey);
}
