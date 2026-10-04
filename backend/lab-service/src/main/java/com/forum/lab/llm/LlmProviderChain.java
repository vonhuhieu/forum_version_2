package com.forum.lab.llm;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class LlmProviderChain {

    @Value("${lab.llm.gemini.api-key:}")
    private String geminiApiKey;
    @Value("${lab.llm.gemini.model:gemini-1.5-flash}")
    private String geminiModel;
    @Value("${lab.llm.gemini.base-url:https://generativelanguage.googleapis.com/v1beta/openai}")
    private String geminiBaseUrl;

    @Value("${lab.llm.groq.api-key:}")
    private String groqApiKey;
    @Value("${lab.llm.groq.model:llama-3.3-70b-versatile}")
    private String groqModel;
    @Value("${lab.llm.groq.base-url:https://api.groq.com/openai/v1}")
    private String groqBaseUrl;

    @Value("${lab.llm.openrouter.api-key:}")
    private String openrouterApiKey;
    @Value("${lab.llm.openrouter.model:meta-llama/llama-3.3-70b-instruct:free}")
    private String openrouterModel;
    @Value("${lab.llm.openrouter.base-url:https://openrouter.ai/api/v1}")
    private String openrouterBaseUrl;

    private final MockLlmProvider mockLlmProvider;
    private final List<LlmProvider> providers = new ArrayList<>();

    @PostConstruct
    public void init() {
        if (geminiApiKey != null && !geminiApiKey.isBlank()) {
            providers.add(new OpenAiCompatibleProvider("Google Gemini", geminiBaseUrl, geminiApiKey, geminiModel));
            log.info("Khởi tạo thành công LLM Provider: Google Gemini ({})", geminiModel);
        }
        if (groqApiKey != null && !groqApiKey.isBlank()) {
            providers.add(new OpenAiCompatibleProvider("Groq", groqBaseUrl, groqApiKey, groqModel));
            log.info("Khởi tạo thành công LLM Provider: Groq ({})", groqModel);
        }
        if (openrouterApiKey != null && !openrouterApiKey.isBlank()) {
            providers.add(new OpenAiCompatibleProvider("OpenRouter Free", openrouterBaseUrl, openrouterApiKey, openrouterModel));
            log.info("Khởi tạo thành công LLM Provider: OpenRouter ({})", openrouterModel);
        }
        // Fallback an toàn luôn có mặt
        providers.add(mockLlmProvider);
        log.info("Tổng số LLM Providers sẵn sàng phục vụ: {}", providers.size());
    }

    public Flux<String> streamChat(List<LlmMessage> messages) {
        return executeWithFallback(messages, 0);
    }

    private Flux<String> executeWithFallback(List<LlmMessage> messages, int index) {
        if (index >= providers.size()) {
            return mockLlmProvider.streamChat(messages);
        }

        LlmProvider currentProvider = providers.get(index);
        if (!currentProvider.isAvailable()) {
            return executeWithFallback(messages, index + 1);
        }

        log.debug("Đang gọi LLM Provider: {}", currentProvider.getProviderName());
        return currentProvider.streamChat(messages)
                .onErrorResume(err -> {
                    log.warn("Nhà cung cấp AI {} gặp lỗi ({}), tự động chuyển sang nhà cung cấp kế tiếp trong chuỗi.",
                            currentProvider.getProviderName(), err.getMessage());
                    return executeWithFallback(messages, index + 1);
                });
    }
}
