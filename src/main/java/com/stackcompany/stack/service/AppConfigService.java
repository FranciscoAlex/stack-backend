package com.stackcompany.stack.service;

import com.stackcompany.stack.entity.AppConfig;
import com.stackcompany.stack.repository.AppConfigRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AppConfigService {

    private final AppConfigRepository repo;

    @PostConstruct
    public void seedDefaults() {
        seed("bot_enabled",          "true",                         "Enable/disable the news bot");
        seed("bot_articles_per_day", "2",                            "Max posts per scheduled run");
        seed("bot_cron",             "0 0 8 * * *",                  "Cron expression (Africa/Luanda)");
        seed("bot_account_email",    "cisi.dg@ucan.edu",             "Bot author email");
        seed("bot_post_prefix",      "",                             "Prefix prepended to every bot post");
        seed("openrouter_api_key",   "",                             "OpenRouter API key");
        seed("openrouter_model",     "google/gemini-2.5-flash-lite", "OpenRouter model ID");
        seed("font_family",          "Inter",                        "Main UI font family");
    }

    private void seed(String key, String value, String desc) {
        if (!repo.existsById(key)) {
            repo.save(AppConfig.builder().configKey(key).configValue(value).description(desc).build());
        }
    }

    public String get(String key, String defaultValue) {
        return repo.findById(key).map(AppConfig::getConfigValue).orElse(defaultValue);
    }

    public boolean getBoolean(String key, boolean defaultValue) {
        String v = get(key, null);
        return v == null ? defaultValue : Boolean.parseBoolean(v.trim());
    }

    public int getInt(String key, int defaultValue) {
        try { return Integer.parseInt(get(key, String.valueOf(defaultValue)).trim()); }
        catch (NumberFormatException e) { return defaultValue; }
    }

    public List<AppConfig> getAll() {
        return repo.findAll();
    }

    @Transactional
    public void set(String key, String value) {
        AppConfig cfg = repo.findById(key).orElseGet(() -> AppConfig.builder().configKey(key).build());
        cfg.setConfigValue(value);
        repo.save(cfg);
    }

    @Transactional
    public void setAll(Map<String, String> values) {
        values.forEach(this::set);
    }
}
