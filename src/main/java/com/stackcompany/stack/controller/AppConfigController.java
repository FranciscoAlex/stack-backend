package com.stackcompany.stack.controller;

import com.stackcompany.stack.entity.AppConfig;
import com.stackcompany.stack.service.AppConfigService;
import com.stackcompany.stack.service.NewsScraperScheduler;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class AppConfigController {

    private final AppConfigService appConfigService;
    private final NewsScraperScheduler newsScraperScheduler;

    @GetMapping("/api/admin/config")
    public ResponseEntity<List<AppConfig>> getAll() {
        return ResponseEntity.ok(appConfigService.getAll());
    }

    @PutMapping("/api/admin/config")
    public ResponseEntity<Void> setAll(@RequestBody Map<String, String> values) {
        appConfigService.setAll(values);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/api/admin/config/{key}")
    public ResponseEntity<Void> set(@PathVariable String key, @RequestBody Map<String, String> body) {
        appConfigService.set(key, body.get("value"));
        return ResponseEntity.ok().build();
    }

    @GetMapping("/api/config/public/{key}")
    public ResponseEntity<Map<String, String>> getPublic(@PathVariable String key) {
        String value = appConfigService.get(key, "");
        return ResponseEntity.ok(Map.of("key", key, "value", value));
    }

    @PostMapping("/api/admin/config/bot/trigger")
    public ResponseEntity<Map<String, String>> triggerBot() {
        try {
            newsScraperScheduler.triggerManualScrape();
            return ResponseEntity.ok(Map.of("status", "OK", "message", "Bot triggered successfully."));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of("status", "ERROR", "message", e.getMessage()));
        }
    }
}
