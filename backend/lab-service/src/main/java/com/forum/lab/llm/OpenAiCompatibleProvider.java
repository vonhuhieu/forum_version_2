package com.forum.lab.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class OpenAiCompatibleProvider implements LlmProvider {

    private final String providerName;
    private final String apiKey;
    private final String model;
    private final WebClient webClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public OpenAiCompatibleProvider(String providerName, String baseUrl, String apiKey, String model) {
        this.providerName = providerName;
        this.apiKey = apiKey;
        this.model = model;
        this.webClient = WebClient.builder()
                .baseUrl(baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    @Override
    public String getProviderName() {
        return providerName;
    }

    @Override
    public boolean isAvailable() {
        return StringUtils.hasText(apiKey) && StringUtils.hasText(model);
    }

    @Override
    public Flux<String> streamChat(List<LlmMessage> messages) {
        if (!isAvailable()) {
            return Flux.error(new IllegalStateException("Nhà cung cấp AI " + providerName + " chưa được cấu hình API Key."));
        }

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", model);
        requestBody.put("messages", messages);
        requestBody.put("stream", true);
        requestBody.put("temperature", 0.7);

        return webClient.post()
                .uri("/chat/completions")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .bodyValue(requestBody)
                .accept(MediaType.TEXT_EVENT_STREAM)
                .retrieve()
                .bodyToFlux(String.class)
                .flatMap(rawLine -> {
                    try {
                        String line = rawLine.trim();
                        if (line.startsWith("data:")) {
                            line = line.substring(5).trim();
                        }
                        if (line.isEmpty() || "[DONE]".equals(line)) {
                            return Flux.empty();
                        }
                        JsonNode root = objectMapper.readTree(line);
                        JsonNode choices = root.path("choices");
                        if (choices.isArray() && !choices.isEmpty()) {
                            JsonNode delta = choices.get(0).path("delta");
                            if (delta.has("content")) {
                                String text = delta.get("content").asText();
                                return Flux.just(text);
                            }
                        }
                    } catch (Exception e) {
                        log.debug("Bỏ qua dòng SSE không parse được: {}", rawLine);
                    }
                    return Flux.empty();
                })
                .onErrorResume(e -> {
                    log.error("Lỗi khi stream chat từ nhà cung cấp {}: {}", providerName, e.getMessage());
                    return Flux.error(e);
                });
    }
}
