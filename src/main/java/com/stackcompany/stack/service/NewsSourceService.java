package com.stackcompany.stack.service;

import com.stackcompany.stack.dto.NewsSourceDto;
import com.stackcompany.stack.entity.NewsSource;
import com.stackcompany.stack.repository.NewsSourceRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
@Slf4j
public class NewsSourceService {

    private final NewsSourceRepository repo;

    @PostConstruct
    public void seedLegacySources() {
        seedIfAbsent("Expansão Angola", "https://expansao.co.ao", "https://expansao.co.ao/economia/",          true);
        seedIfAbsent("O Telegrama",     "https://www.otelegrama.ao", "https://www.otelegrama.ao/empresasmercado/", true);
    }

    private void seedIfAbsent(String name, String baseUrl, String listUrl, boolean legacy) {
        if (repo.existsByListUrl(listUrl)) return;
        repo.save(NewsSource.builder().name(name).baseUrl(baseUrl).listUrl(listUrl)
                .legacy(legacy).enabled(true).articlesPerRun(2).status("ACTIVE").build());
        log.info("Seeded news source: {}", name);
    }

    public List<NewsSourceDto> getAll() { return repo.findAll().stream().map(this::toDto).toList(); }

    public NewsSourceDto getById(Long id) { return toDto(findOrThrow(id)); }

    @Transactional
    public NewsSourceDto create(NewsSourceDto dto) {
        if (repo.existsByListUrl(dto.getListUrl()))
            throw new IllegalArgumentException("A source with this list URL already exists.");
        return toDto(repo.save(fromDto(dto)));
    }

    @Transactional
    public NewsSourceDto update(Long id, NewsSourceDto dto) {
        NewsSource s = findOrThrow(id);
        s.setName(dto.getName());
        s.setBaseUrl(dto.getBaseUrl());
        s.setListUrl(dto.getListUrl());
        s.setEnabled(dto.isEnabled());
        s.setArticlesPerRun(dto.getArticlesPerRun() > 0 ? dto.getArticlesPerRun() : s.getArticlesPerRun());
        if (dto.getSelectorsJson() != null) s.setSelectorsJson(dto.getSelectorsJson());
        if (dto.getStatus() != null) s.setStatus(dto.getStatus());
        return toDto(repo.save(s));
    }

    @Transactional
    public void delete(Long id) { repo.deleteById(id); }

    @Transactional
    public void updateStatus(Long id, String status, String lastError) {
        repo.findById(id).ifPresent(s -> {
            s.setStatus(status); s.setLastError(lastError); s.setLastScrapedAt(LocalDateTime.now());
            repo.save(s);
        });
    }

    public NewsSource getEntityById(Long id) { return findOrThrow(id); }

    public List<NewsSource> getEnabledSources() { return repo.findByEnabledTrueOrderByIdAsc(); }

    public NewsSourceDto toDto(NewsSource s) {
        return NewsSourceDto.builder().id(s.getId()).name(s.getName()).baseUrl(s.getBaseUrl())
                .listUrl(s.getListUrl()).enabled(s.isEnabled()).articlesPerRun(s.getArticlesPerRun())
                .selectorsJson(s.getSelectorsJson()).legacy(s.isLegacy()).status(s.getStatus())
                .lastScrapedAt(s.getLastScrapedAt() != null ? s.getLastScrapedAt().toString() : null)
                .lastError(s.getLastError()).build();
    }

    private NewsSource fromDto(NewsSourceDto dto) {
        return NewsSource.builder().name(dto.getName()).baseUrl(dto.getBaseUrl()).listUrl(dto.getListUrl())
                .enabled(dto.isEnabled()).articlesPerRun(dto.getArticlesPerRun() > 0 ? dto.getArticlesPerRun() : 2)
                .selectorsJson(dto.getSelectorsJson()).legacy(dto.isLegacy())
                .status(dto.getSelectorsJson() != null && !dto.getSelectorsJson().isBlank() ? "ACTIVE" : "NEEDS_CONFIG")
                .build();
    }

    private NewsSource findOrThrow(Long id) {
        return repo.findById(id).orElseThrow(() -> new NoSuchElementException("Source not found: " + id));
    }
}
