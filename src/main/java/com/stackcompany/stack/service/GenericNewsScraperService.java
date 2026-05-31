package com.stackcompany.stack.service;

import com.stackcompany.stack.dto.NewsArticle;
import com.stackcompany.stack.entity.NewsSource;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Slf4j
public class GenericNewsScraperService {

    private final ObjectMapper objectMapper;
    private static final int TIMEOUT_MS = 30_000;
    private static final String UA =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/120.0.0.0 Safari/537.36";

    public List<NewsArticle> scrape(NewsSource source, int limit) {
        List<NewsArticle> results = new ArrayList<>();
        if (source.getSelectorsJson() == null || source.getSelectorsJson().isBlank()) return results;
        try {
            JsonNode sel = objectMapper.readTree(source.getSelectorsJson());
            String articleLinksSelector = text(sel, "articleLinks");
            String titleSelector        = text(sel, "title");
            String bodySelector         = text(sel, "body");
            String imageSelector        = text(sel, "image");
            if (articleLinksSelector == null || articleLinksSelector.isBlank()) return results;

            Document listPage = Jsoup.connect(source.getListUrl()).userAgent(UA).timeout(TIMEOUT_MS).get();
            Set<String> urls = new LinkedHashSet<>();
            for (Element el : listPage.select(articleLinksSelector)) {
                String href = el.attr("abs:href");
                if (href.isBlank()) href = el.attr("href");
                if (!href.isBlank() && !href.equals(source.getListUrl()) && urls.size() < limit + 3)
                    urls.add(href.split("\\?")[0]);
            }
            for (String articleUrl : urls) {
                if (results.size() >= limit) break;
                try {
                    NewsArticle a = scrapeArticle(source, articleUrl, titleSelector, bodySelector, imageSelector);
                    if (a != null) results.add(a);
                } catch (Exception e) {
                    log.debug("Failed article {}: {}", articleUrl, e.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("Failed source '{}': {}", source.getName(), e.getMessage());
        }
        return results;
    }

    private NewsArticle scrapeArticle(NewsSource source, String url,
            String titleSel, String bodySel, String imageSel) throws Exception {
        Document doc = Jsoup.connect(url).userAgent(UA).timeout(TIMEOUT_MS).get();
        String title = selectText(doc, titleSel);
        if (title == null || title.isBlank()) title = doc.title();
        if (title == null || title.isBlank()) return null;
        String body = selectText(doc, bodySel);
        if (body == null || body.isBlank())
            body = doc.select("article p, .content p, main p").text();
        String imageUrl = null;
        if (imageSel != null && !imageSel.isBlank()) {
            Element img = doc.selectFirst(imageSel);
            if (img != null) { imageUrl = img.attr("abs:src"); if (imageUrl.isBlank()) imageUrl = img.attr("src"); }
        }
        if (imageUrl == null || imageUrl.isBlank()) {
            Element img = doc.selectFirst("article img, .content img, main img");
            if (img != null) imageUrl = img.attr("abs:src");
        }
        return NewsArticle.builder()
                .title(title.trim()).body(body != null ? body.trim() : "")
                .imageUrl(imageUrl).sourceUrl(url).sourceName(source.getName()).build();
    }

    public List<String> previewTitles(NewsSource source, int limit) {
        if (source.getSelectorsJson() == null || source.getSelectorsJson().isBlank()) return List.of();
        try {
            JsonNode sel = objectMapper.readTree(source.getSelectorsJson());
            String articleLinksSelector = text(sel, "articleLinks");
            String titleSelector = text(sel, "title");
            if (articleLinksSelector == null) return List.of();
            Document listPage = Jsoup.connect(source.getListUrl()).userAgent(UA).timeout(TIMEOUT_MS).get();
            Elements links = listPage.select(articleLinksSelector);
            List<String> titles = new ArrayList<>();
            for (Element link : links) {
                if (titles.size() >= limit) break;
                String t = link.text().trim();
                if (t.isBlank() && titleSelector != null) {
                    Element child = link.selectFirst(titleSelector);
                    if (child != null) t = child.text().trim();
                }
                if (!t.isBlank()) titles.add(t);
            }
            return titles;
        } catch (Exception e) { return List.of(); }
    }

    public String fetchHtml(String url) throws Exception {
        Document doc = Jsoup.connect(url).userAgent(UA).timeout(TIMEOUT_MS).get();
        doc.select("script, style, svg, footer, nav, iframe, noscript, head").remove();
        String html = doc.body().html();
        return html.length() > 12_000 ? html.substring(0, 12_000) + "\n... [truncated]" : html;
    }

    private String text(JsonNode node, String field) {
        JsonNode n = node.get(field);
        return (n != null && !n.isNull()) ? n.asText() : null;
    }

    private String selectText(Document doc, String selector) {
        if (selector == null || selector.isBlank()) return null;
        try { Element el = doc.selectFirst(selector); return el != null ? el.text() : null; }
        catch (Exception e) { return null; }
    }
}
