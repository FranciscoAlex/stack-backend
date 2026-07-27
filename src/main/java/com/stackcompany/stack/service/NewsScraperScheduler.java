package com.stackcompany.stack.service;

import com.stackcompany.stack.dto.NewsArticle;
import com.stackcompany.stack.entity.NewsSource;
import com.stackcompany.stack.entity.Post;
import com.stackcompany.stack.entity.ScrapedArticle;
import com.stackcompany.stack.entity.Tag;
import com.stackcompany.stack.entity.User;
import com.stackcompany.stack.repository.PostRepository;
import com.stackcompany.stack.repository.ScrapedArticleRepository;
import com.stackcompany.stack.repository.TagRepository;
import com.stackcompany.stack.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class NewsScraperScheduler {

    private final AppConfigService          appConfig;
    private final NewsScraperService        expansaoScraper;
    private final TelegramaScraperService   telegramaScraper;
    private final GenericNewsScraperService genericScraper;
    private final NewsSourceService         newsSourceService;
    private final OpenRouterService         openRouter;
    private final UserRepository            userRepository;
    private final PostRepository            postRepository;
    private final TagRepository             tagRepository;
    private final ScrapedArticleRepository  scrapedArticleRepository;

    @Scheduled(cron = "${scraper.cron:0 0 8 * * *}", zone = "Africa/Luanda")
    public void scrapeAndPostNews() {
        boolean enabled = appConfig.getBoolean("bot_enabled", true);
        if (!enabled) { log.info("News bot is disabled."); return; }

        int articlesPerDay   = appConfig.getInt("bot_articles_per_day", 2);
        String botEmails     = appConfig.get("bot_account_email", "cisi.dg@ucan.edu");

        log.info("Starting scheduled news scraping (max {} posts)...", articlesPerDay);

        try {
            List<User> botUsers = resolveBotAuthors(botEmails);
            if (botUsers.isEmpty()) {
                Optional<User> fallback = userRepository.findFirstByRule("ADMIN");
                if (fallback.isEmpty()) { log.error("No bot or ADMIN user found."); return; }
                botUsers = new ArrayList<>(List.of(fallback.get()));
                log.warn("No configured bot account matched a user; falling back to ADMIN '{}'.",
                        fallback.get().getEmail());
            }
            User botUser = botUsers.get(0);

            // Legacy fill-in: only when a single author is configured do we spread the
            // remaining posts across other admins. With an explicit multi-account list
            // we rotate strictly within that list instead.
            List<User> admins = new ArrayList<>();
            if (botUsers.size() == 1) {
                admins.addAll(userRepository.findAllByRule("ADMIN"));
                admins.removeIf(u -> u.getId().equals(botUser.getId()));
            }

            // ── Scrape all enabled sources ──────────────────────────────────────
            List<NewsArticle> articles = new ArrayList<>();
            List<NewsSource> sources;
            try {
                sources = newsSourceService.getEnabledSources();
            } catch (Exception e) {
                log.warn("Could not load DB sources ({}), falling back to legacy scrapers.", e.getMessage());
                sources = List.of();
                articles.addAll(expansaoScraper.scrapeLatestNews(articlesPerDay + 2));
                articles.addAll(telegramaScraper.scrapeLatestNews(articlesPerDay + 2));
            }

            for (NewsSource src : sources) {
                try {
                    List<NewsArticle> sourced;
                    if (src.isLegacy()) {
                        if (src.getName().toLowerCase().contains("expans")) {
                            sourced = expansaoScraper.scrapeLatestNews(src.getArticlesPerRun() + 2);
                        } else if (src.getName().toLowerCase().contains("telegrama")) {
                            sourced = telegramaScraper.scrapeLatestNews(src.getArticlesPerRun() + 2);
                        } else {
                            sourced = List.of();
                        }
                    } else {
                        sourced = genericScraper.scrape(src, src.getArticlesPerRun() + 2);
                    }
                    articles.addAll(sourced);
                    if (!sourced.isEmpty()) newsSourceService.updateStatus(src.getId(), "ACTIVE", null);
                } catch (Exception e) {
                    log.warn("Source '{}' failed: {}", src.getName(), e.getMessage());
                    try { newsSourceService.updateStatus(src.getId(), "ERROR", e.getMessage()); } catch (Exception ignored) {}
                }
            }

            if (articles.isEmpty()) { log.warn("No articles found."); return; }

            // ── Deduplicate ─────────────────────────────────────────────────────
            List<NewsArticle> fresh = new ArrayList<>();
            for (NewsArticle a : articles) {
                if (isAlreadyPosted(a.getTitle(), a.getSourceUrl())) {
                    recordScraped(a, "DUPLICATE", null);
                } else {
                    fresh.add(a);
                }
            }
            if (fresh.isEmpty()) { log.warn("All articles are duplicates."); return; }

            // ── AI picks best ───────────────────────────────────────────────────
            int bestIdx = openRouter.selectBestArticleIndex(fresh);
            if (bestIdx > 0 && bestIdx < fresh.size()) {
                fresh.add(0, fresh.remove(bestIdx));
            }

            // ── Post ─────────────────────────────────────────────────────────────
            int posted = 0;
            for (int i = 0; i < fresh.size() && posted < articlesPerDay; i++) {
                NewsArticle article = fresh.get(i);
                User author;
                if (botUsers.size() > 1) {
                    author = botUsers.get(posted % botUsers.size());
                } else {
                    author = (i == 0) ? botUser : (admins.isEmpty() ? botUser : admins.get(i % admins.size()));
                }
                try {
                    // For Expansão: try to fetch full article text
                    if ("Expansão Angola".equals(article.getSourceName()) && article.getSourceUrl() != null) {
                        Optional<NewsArticle> full = expansaoScraper.scrapeFullArticle(article.getSourceUrl());
                        if (full.isPresent() && full.get().getBody() != null
                                && full.get().getBody().length() > article.getBody().length()) {
                            NewsArticle f = full.get();
                            if (f.getImageUrl() == null) f.setImageUrl(article.getImageUrl());
                            article = f;
                        }
                    }
                    Post post = createPost(author, article);
                    recordScraped(article, "POSTED", post.getId());
                    posted++;
                } catch (Exception e) {
                    log.error("Failed to post '{}': {}", article.getTitle(), e.getMessage());
                    try { recordScraped(article, "FAILED", null); } catch (Exception ignored) {}
                }
            }
            log.info("Scraping done. Created {} posts.", posted);

        } catch (Exception e) {
            log.error("Scraping task failed: {}", e.getMessage(), e);
        }
    }

    /**
     * Resolves the configured bot author(s). Accepts a single email or a comma/semicolon
     * separated list, so several accounts can take turns publishing. Unknown emails are
     * skipped with a warning rather than aborting the run.
     */
    private List<User> resolveBotAuthors(String configured) {
        List<User> authors = new ArrayList<>();
        if (configured == null || configured.isBlank()) return authors;

        for (String raw : configured.split("[,;]")) {
            String email = raw.trim();
            if (email.isEmpty()) continue;

            Optional<User> found = userRepository.findByEmail(email);
            if (found.isEmpty()) {
                log.warn("Bot account '{}' not found in the database — skipping.", email);
                continue;
            }
            User user = found.get();
            boolean duplicate = authors.stream().anyMatch(u -> u.getId().equals(user.getId()));
            if (!duplicate) authors.add(user);
        }

        if (!authors.isEmpty()) {
            log.info("Bot will publish as {} account(s): {}", authors.size(),
                    authors.stream().map(User::getEmail).toList());
        }
        return authors;
    }

    @Transactional
    protected Post createPost(User user, NewsArticle article) {
        String content = openRouter.summarizeForPost(article.getTitle(), article.getBody());
        String prefix  = appConfig.get("bot_post_prefix", "");
        if (prefix != null && !prefix.isBlank()) content = prefix.trim() + "\n\n" + content;
        if (article.getSourceUrl() != null && !article.getSourceUrl().isBlank())
            content = content + "\n\n🔗 Fonte: " + article.getSourceName();

        Post post = Post.builder()
                .user(user).content(content).votes(0).commentsCount(0)
                .sharesCount(0).views(0).status("APPROVED").pinned(false).build();
        if (article.getImageUrl() != null && !article.getImageUrl().isBlank())
            post.setImageUrl(article.getImageUrl());
        post.setTags(getOrCreateTags(List.of("Economia", "Angola", "Notícias")));
        postRepository.save(post);
        postRepository.flush();
        return post;
    }

    private boolean isAlreadyPosted(String title, String sourceUrl) {
        if (sourceUrl != null) {
            if (scrapedArticleRepository.existsByUrlHash(ScrapedArticle.generateHash(sourceUrl))) return true;
            if (scrapedArticleRepository.existsBySourceUrl(sourceUrl)) return true;
        }
        if (title != null && scrapedArticleRepository.existsByTitleHash(ScrapedArticle.generateHash(title))) return true;
        if (title != null) {
            String term = title.length() > 50 ? title.substring(0, 50) : title;
            if (!postRepository.findRecentByContentContaining(term.toLowerCase(), LocalDateTime.now().minusDays(30)).isEmpty())
                return true;
        }
        return false;
    }

    @Transactional
    protected ScrapedArticle recordScraped(NewsArticle article, String status, Long postId) {
        return scrapedArticleRepository.save(ScrapedArticle.builder()
                .sourceUrl(article.getSourceUrl())
                .urlHash(ScrapedArticle.generateHash(article.getSourceUrl()))
                .title(article.getTitle())
                .titleHash(ScrapedArticle.generateHash(article.getTitle()))
                .sourceName(article.getSourceName()).status(status).postId(postId)
                .scrapedAt(LocalDateTime.now())
                .postedAt("POSTED".equals(status) ? LocalDateTime.now() : null).build());
    }

    private List<Tag> getOrCreateTags(List<String> names) {
        List<Tag> tags = new ArrayList<>();
        for (String name : names) {
            Tag t = tagRepository.findByName(name)
                    .orElseGet(() -> tagRepository.save(Tag.builder().name(name).build()));
            tags.add(t);
        }
        return tags;
    }

    public void triggerManualScrape() { scrapeAndPostNews(); }
}
