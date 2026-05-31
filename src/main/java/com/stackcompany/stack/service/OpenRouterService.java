package com.stackcompany.stack.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stackcompany.stack.dto.NewsArticle;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class OpenRouterService {

    private static final String API_URL       = "https://openrouter.ai/api/v1/chat/completions";
    private static final String DEFAULT_MODEL  = "google/gemini-2.5-flash-lite";

    @Value("${openrouter.api-key:}")
    private String apiKey;

    @Value("${openrouter.model:google/gemini-2.5-flash-lite}")
    private String model;

    private final RestTemplate  restTemplate = new RestTemplate();
    private final ObjectMapper  objectMapper = new ObjectMapper();

    // ── Public API ─────────────────────────────────────────────────────────────

    public String summarizeForPost(String title, String body) {
        if (!isConfigured()) return formatDefault(title, body);
        try {
            String prompt = """
                És um jornalista profissional angolano. Transforma a seguinte notícia num post informativo e envolvente para uma rede social profissional de finanças e economia.

                Título original: %s

                Conteúdo original: %s

                Instruções:
                - Escreve em português de Angola
                - Mantém o tom profissional mas acessível
                - Inclui os pontos principais da notícia
                - Adiciona 2-3 hashtags relevantes no final (ex: #EconomiaAngola #Finanças)
                - O post deve ter entre 200-400 caracteres
                - Não incluas emojis
                - Começa directamente com o conteúdo, sem cumprimentos

                Responde apenas com o texto do post, sem explicações adicionais.
                """.formatted(title, body);
            String resp = call(prompt, apiKey, model, 500, 0.7);
            return resp != null && !resp.isBlank() ? resp.trim() : formatDefault(title, body);
        } catch (Exception e) {
            log.error("summarizeForPost failed: {}", e.getMessage());
            return formatDefault(title, body);
        }
    }

    public int selectBestArticleIndex(List<NewsArticle> articles) {
        if (articles == null || articles.size() <= 1 || !isConfigured()) return 0;
        try {
            StringBuilder prompt = new StringBuilder(
                "És um analista financeiro senior em Angola. Das seguintes notícias, indica qual é a mais importante e relevante para investidores e para o mercado financeiro. Responde APENAS com o número correspondente à melhor notícia, sem qualquer outro texto.\n\n");
            for (int i = 0; i < articles.size(); i++) {
                NewsArticle a = articles.get(i);
                String bodySnippet = a.getBody() != null && a.getBody().length() > 300
                        ? a.getBody().substring(0, 300) + "..." : a.getBody();
                prompt.append("Notícia ").append(i).append(":\nTítulo: ").append(a.getTitle())
                      .append("\nResumo: ").append(bodySnippet).append("\n\n");
            }
            String resp = call(prompt.toString(), apiKey, model, 10, 0.1);
            if (resp != null) {
                String num = resp.replaceAll("[^0-9]", "");
                if (!num.isEmpty()) {
                    int idx = Integer.parseInt(num);
                    if (idx >= 0 && idx < articles.size()) return idx;
                }
            }
        } catch (Exception e) {
            log.error("selectBestArticle failed: {}", e.getMessage());
        }
        return 0;
    }

    public String analyzePageSelectors(String html, String pageUrl, String keyOverride, String modelOverride) {
        String effectiveKey   = (keyOverride != null && !keyOverride.isBlank())   ? keyOverride   : apiKey;
        String effectiveModel = (modelOverride != null && !modelOverride.isBlank()) ? modelOverride : resolveModel();
        if (effectiveKey == null || effectiveKey.isBlank()) { log.warn("analyzePageSelectors: no API key"); return null; }

        String prompt = """
                You are an expert web scraper. Analyze the following HTML from a news website (%s).
                Return ONLY a valid JSON object (no markdown) with these keys:
                - "articleLinks": CSS selector for <a> elements linking to articles on the list page
                - "title": CSS selector for article title on detail page
                - "body": CSS selector for article body text on detail page
                - "image": CSS selector for main image on detail page (or null)
                If unsure, use null. Respond with ONLY the JSON.

                HTML (truncated):
                %s
                """.formatted(pageUrl, html);
        try {
            String content = call(prompt, effectiveKey, effectiveModel, 300, 0.1);
            if (content != null) {
                content = content.replaceAll("(?s)^```(?:json)?\\s*", "").replaceAll("(?s)\\s*```$", "").trim();
                objectMapper.readTree(content); // validate JSON
                return content;
            }
        } catch (Exception e) { log.error("analyzePageSelectors failed: {}", e.getMessage()); }
        return null;
    }

    public boolean isConfigured() { return apiKey != null && !apiKey.isBlank(); }

    // ── Private ────────────────────────────────────────────────────────────────

    private String resolveModel() { return (model != null && !model.isBlank()) ? model : DEFAULT_MODEL; }

    private String call(String prompt, String key, String mdl, int maxTokens, double temp) throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(key);
        headers.set("HTTP-Referer", "https://stakesomosholdrs.com");
        headers.set("X-Title", "Stack Platform");

        Map<String, Object> body = new HashMap<>();
        body.put("model", mdl != null && !mdl.isBlank() ? mdl : DEFAULT_MODEL);
        body.put("messages", List.of(Map.of("role", "user", "content", prompt)));
        body.put("max_tokens", maxTokens);
        body.put("temperature", temp);

        ResponseEntity<String> resp = restTemplate.exchange(API_URL, HttpMethod.POST,
                new HttpEntity<>(objectMapper.writeValueAsString(body), headers), String.class);

        if (resp.getStatusCode().is2xxSuccessful() && resp.getBody() != null) {
            JsonNode root = objectMapper.readTree(resp.getBody());
            JsonNode choices = root.get("choices");
            if (choices != null && choices.isArray() && !choices.isEmpty())
                return choices.get(0).get("message").get("content").asText();
        }
        return null;
    }

    private String formatDefault(String title, String body) {
        String truncated = body != null && body.length() > 300 ? body.substring(0, 297) + "..." : body;
        return "📰 " + title + "\n\n" + (truncated != null ? truncated : "") + "\n\n#EconomiaAngola #Notícias";
    }
}
