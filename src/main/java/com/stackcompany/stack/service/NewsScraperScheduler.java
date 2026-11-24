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
        String botEmail      = appConfig.get("bot_account_email", "cisi.dg@ucan.edu");

        log.info("Starting scheduled news scraping (max {} posts)...", articlesPerDay);

        try {
            Optional<User> botOpt = userRepository.findByEmail(botEmail);
            if (botOpt.isEmpty()) botOpt = userRepository.findFirstByRule("ADMIN");
            if (botOpt.isEmpty()) { log.error("No bot or ADMIN user found."); return; }
            User botUser = botOpt.get();

            List<User> admins = new ArrayList<>(userRepository.findAllByRule("ADMIN"));
            admins.removeIf(u -> u.getId().equals(botUser.getId()));

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
                User author = (i == 0) ? botUser : (admins.isEmpty() ? botUser : admins.get(i % admins.size()));
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
     * Post text for a scraped article: the full article rewritten by the AI (see
     * OpenRouterService.writeFullArticle), or the old short excerpt when the AI is unavailable,
     * plus the configured prefix and "🔗 Fonte: <name> (<url>)" so readers can open the original.
     */
    private String postContent(NewsArticle article) {
        String content = openRouter.writeFullArticle(article.getTitle(), article.getBody());
        if (content == null) content = openRouter.summarizeForPost(article.getTitle(), article.getBody());
        String prefix  = appConfig.get("bot_post_prefix", "");
        if (prefix != null && !prefix.isBlank()) content = prefix.trim() + "\n\n" + content;
        if (article.getSourceUrl() != null && !article.getSourceUrl().isBlank())
            content = content + "\n\n🔗 Fonte: " + article.getSourceName() + " (" + article.getSourceUrl() + ")";
        return content;
    }

    @Transactional
    protected Post createPost(User user, NewsArticle article) {
        String content = postContent(article);

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

    // ── Rebuild old short posts in full ───────────────────────────────────────
    // Until 2026-11 the AI step was told to write 200-400 characters, so news posts were saved as
    // short summaries (median 454 chars). This job re-reads each original article (scraped_article
    // keeps the source URL and post id) and replaces the post text with the full article, a few
    // posts per run so the sources and the AI are not flooded. Votes, comments, views and the
    // image stay. Posts already long enough, or edited so they no longer end with a "Fonte:" line,
    // are left alone; it stops by itself once nothing is left. OFF by default (owner: only new posts are
    // written in full, old posts stay as they are); scraper.upgrade-short-posts=true turns it on.

    private static final int FULL_POST_CHARS = 1500;
    private static final int UPGRADES_PER_RUN = 5;
    private final java.util.Set<Long> upgradeSkipped = java.util.concurrent.ConcurrentHashMap.newKeySet();

    @org.springframework.beans.factory.annotation.Value("${scraper.upgrade-short-posts:false}")
    private boolean upgradeShortPosts;

    @Scheduled(initialDelayString = "${scraper.upgrade.initial-delay-ms:120000}", fixedDelayString = "${scraper.upgrade.delay-ms:300000}")
    public void upgradeShortPosts() {
        if (!upgradeShortPosts || !openRouter.isConfigured()) return;
        int done = 0;
        for (ScrapedArticle sa : scrapedArticleRepository.findTop200ByStatusAndPostIdIsNotNullOrderByIdDesc("POSTED")) {
            if (done >= UPGRADES_PER_RUN) break;
            if (sa.getSourceUrl() == null || sa.getSourceUrl().isBlank() || upgradeSkipped.contains(sa.getId())) continue;
            Optional<Post> found = postRepository.findById(sa.getPostId());
            if (found.isEmpty()) { upgradeSkipped.add(sa.getId()); continue; }
            Post post = found.get();
            String current = post.getContent() == null ? "" : post.getContent();
            if (current.length() >= FULL_POST_CHARS || !current.contains("Fonte:")) { upgradeSkipped.add(sa.getId()); continue; }
            try {
                Optional<NewsArticle> article = rescrape(sa);
                if (article.isEmpty() || article.get().getBody() == null || article.get().getBody().length() < 400) {
                    log.info("Upgrade: original unavailable or too short for post {} ({})", post.getId(), sa.getSourceUrl());
                    upgradeSkipped.add(sa.getId());
                    continue;
                }
                NewsArticle a = article.get();
                if (a.getSourceName() == null || a.getSourceName().isBlank()) a.setSourceName(sa.getSourceName());
                if (a.getSourceUrl() == null || a.getSourceUrl().isBlank()) a.setSourceUrl(sa.getSourceUrl());
                String full = openRouter.writeFullArticle(a.getTitle(), a.getBody());
                if (full == null) { upgradeSkipped.add(sa.getId()); continue; }
                String prefix = appConfig.get("bot_post_prefix", "");
                String content = (prefix != null && !prefix.isBlank() ? prefix.trim() + "\n\n" : "") + full
                        + "\n\n🔗 Fonte: " + a.getSourceName() + " (" + a.getSourceUrl() + ")";
                post.setContent(content);
                if ((post.getImageUrl() == null || post.getImageUrl().isBlank()) && a.getImageUrl() != null && !a.getImageUrl().isBlank())
                    post.setImageUrl(a.getImageUrl());
                postRepository.save(post);
                done++;
                log.info("Upgrade: post {} rebuilt in full ({} -> {} chars)", post.getId(), current.length(), content.length());
            } catch (Exception e) {
                log.warn("Upgrade: post {} failed: {}", post.getId(), e.getMessage());
                upgradeSkipped.add(sa.getId());
            }
        }
    }

    private Optional<NewsArticle> rescrape(ScrapedArticle sa) {
        String name = sa.getSourceName() == null ? "" : sa.getSourceName();
        String lower = name.toLowerCase();
        if (lower.contains("expans")) return expansaoScraper.scrapeFullArticle(sa.getSourceUrl());
        if (lower.contains("telegrama")) return telegramaScraper.scrapeFullArticle(sa.getSourceUrl());
        return newsSourceService.findByName(name).flatMap(src -> genericScraper.scrapeOne(src, sa.getSourceUrl()));
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
