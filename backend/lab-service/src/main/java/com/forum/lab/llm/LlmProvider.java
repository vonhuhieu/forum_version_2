package com.forum.lab.llm;

import reactor.core.publisher.Flux;

import java.util.List;

public interface LlmProvider {
    String getProviderName();
    boolean isAvailable();
    Flux<String> streamChat(List<LlmMessage> messages);
}
