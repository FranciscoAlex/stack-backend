package com.stackcompany.stack.controller;

import com.stackcompany.stack.dto.NewsSourceDto;
import com.stackcompany.stack.entity.NewsSource;
import com.stackcompany.stack.service.AppConfigService;
import com.stackcompany.stack.service.GenericNewsScraperService;
import com.stackcompany.stack.service.NewsSourceService;
import com.stackcompany.stack.service.OpenRouterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/sources")
@RequiredArgsConstructor
@Slf4j
public class NewsSourceController {

    private final NewsSourceService       newsSourceService;
    private final GenericNewsScraperService scraperService;
    private final OpenRouterService       openRouterService;
    private final AppConfigService        appConfigService;

    @GetMapping
    public ResponseEntity<List<NewsSourceDto>> getAll() {
        return ResponseEntity.ok(newsSourceService.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<NewsSourceDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(newsSourceService.getById(id));
    }

    @PostMapping
    public ResponseEntity<NewsSourceDto> create(@RequestBody NewsSourceDto dto) {
        return ResponseEntity.ok(newsSourceService.create(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<NewsSourceDto> update(@PathVariable Long id, @RequestBody NewsSourceDto dto) {
        return ResponseEntity.ok(newsSourceService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        newsSourceService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/analyze")
    public ResponseEntity<NewsSourceDto> analyze(@RequestBody Map<String, Object> body) {
        String url = (String) body.get("url");
        if (url == null || url.isBlank()) return ResponseEntity.badRequest().build();

        String apiKey = appConfigService.get("openrouter_api_key", "");
        String model  = appConfigService.get("openrouter_model", "google/gemini-2.5-flash-lite");

        NewsSourceDto result = new NewsSourceDto();
        result.setListUrl(url);
        try {
            String html = scraperService.fetchHtml(url);
            String selectorsJson = openRouterService.analyzePageSelectors(html, url, apiKey, model);
            if (selectorsJson == null) {
                result.setAnalyzeMessage("AI analysis failed — configure OpenRouter API key in Settings first.");
                result.setSelectorsJson("{\"articleLinks\":\"\",\"title\":\"\",\"body\":\"\",\"image\":\"\"}");
                return ResponseEntity.ok(result);
            }
            result.setSelectorsJson(selectorsJson);

            Object sourceIdObj = body.get("sourceId");
            if (sourceIdObj != null) {
                try {
                    Long id = Long.valueOf(sourceIdObj.toString());
                    NewsSource src = newsSourceService.getEntityById(id);
                    src.setSelectorsJson(selectorsJson);
                    src.setStatus("ACTIVE");
                    newsSourceService.update(id, newsSourceService.toDto(src));
                    result.setId(id);
                } catch (Exception e) { log.warn("Could not persist selectors for source {}: {}", sourceIdObj, e.getMessage()); }
            }

            NewsSource tmp = NewsSource.builder().listUrl(url).name("preview").baseUrl(url).selectorsJson(selectorsJson).build();
            List<String> titles = scraperService.previewTitles(tmp, 5);
            result.setPreviewTitles(titles);
            result.setAnalyzeMessage(titles.isEmpty()
                    ? "Selectors detected but no titles found — adjust manually."
                    : "AI found " + titles.size() + " article(s). Review and save.");
        } catch (Exception e) {
            log.error("Analyze failed for {}: {}", url, e.getMessage());
            result.setAnalyzeMessage("Failed to fetch page: " + e.getMessage());
            result.setSelectorsJson("{\"articleLinks\":\"\",\"title\":\"\",\"body\":\"\",\"image\":\"\"}");
        }
        return ResponseEntity.ok(result);
    }
}
