package com.stackcompany.stack.service;

import com.stackcompany.stack.dto.NewsArticle;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Scraper for Expansão Angola (https://expansao.co.ao)
 */
@Service
@Slf4j
public class NewsScraperService {

    private static final String BASE_URL    = "https://expansao.co.ao";
    private static final String ECONOMIA_URL = BASE_URL + "/economia/";
    private static final int    TIMEOUT_MS  = 30_000;
    private static final String UA =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/120.0.0.0 Safari/537.36";

    public List<NewsArticle> scrapeLatestNews(int limit) {
        List<NewsArticle> articles = new ArrayList<>();
        try {
            log.info("Scraping Expansão Angola: {}", ECONOMIA_URL);
            Document doc = Jsoup.connect(ECONOMIA_URL).userAgent(UA).timeout(TIMEOUT_MS).get();

            // Collect article links — use contains to handle relative or absolute hrefs
            Elements links = doc.select("a[href*='/economia/']");
            List<String> urls = new ArrayList<>();
            for (Element link : links) {
                String href = link.attr("abs:href");
                if (href.isBlank()) href = link.attr("href");
                if (!href.isBlank() && href.length() > ECONOMIA_URL.length()
                        && !href.endsWith("/economia/")
                        && !href.contains("/page/")
                        && !href.endsWith("feed/")
                        && !urls.contains(href)) {
                    urls.add(href.split("\\?")[0]);
                }
            }
            log.info("Expansão: found {} candidate URLs", urls.size());

            for (String url : urls) {
                if (articles.size() >= limit) break;
                try {
                    scrapeFullArticle(url).ifPresent(a -> {
                        log.info("Scraped Expansão article: {}", a.getTitle());
                        articles.add(a);
                    });
                } catch (Exception e) {
                    log.debug("Failed Expansão article {}: {}", url, e.getMessage());
                }
            }
        } catch (IOException e) {
            log.error("Failed connecting to Expansão: {}", e.getMessage());
        }
        return articles;
    }

    public Optional<NewsArticle> scrapeFullArticle(String url) {
        try {
            Document doc = Jsoup.connect(url).userAgent(UA).timeout(TIMEOUT_MS).get();

            String title = null;
            Element ogTitle = doc.selectFirst("meta[property='og:title']");
            if (ogTitle != null) title = ogTitle.attr("content");
            if (title == null || title.isBlank()) {
                Element h1 = doc.selectFirst("h1");
                if (h1 != null) title = h1.text();
            }
            if (title == null || title.isBlank()) title = doc.title();
            if (title == null || title.length() < 5) return Optional.empty();

            String imageUrl = null;
            Element ogImg = doc.selectFirst("meta[property='og:image']");
            if (ogImg != null) imageUrl = ogImg.attr("content");

            // Collect paragraphs
            Elements paragraphs = doc.select(".entry-content p, article p, .post-content p, main p");
            if (paragraphs.isEmpty()) paragraphs = doc.select("p");
            StringBuilder body = new StringBuilder();
            for (Element p : paragraphs) {
                String text = p.text().trim();
                if (!text.isEmpty() && text.length() > 20) body.append(text).append("\n\n");
            }
            String bodyStr = body.toString().trim();
            if (bodyStr.isEmpty()) {
                Element ogDesc = doc.selectFirst("meta[property='og:description']");
                if (ogDesc != null) bodyStr = ogDesc.attr("content");
            }
            if (bodyStr == null || bodyStr.isBlank()) return Optional.empty();

            return Optional.of(NewsArticle.builder()
                    .title(title.trim()).body(bodyStr)
                    .imageUrl(imageUrl).sourceUrl(url).sourceName("Expansão Angola").build());
        } catch (IOException e) {
            log.error("Failed Expansão article {}: {}", url, e.getMessage());
            return Optional.empty();
        }
    }
}
