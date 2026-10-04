package com.forum.lab.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.forum.lab.document.LabChatSession;
import com.forum.lab.document.LabMessage;
import com.forum.lab.dto.ProductDTO;
import com.forum.lab.llm.LlmMessage;
import com.forum.lab.llm.LlmProviderChain;
import com.forum.lab.repository.LabMessageRepository;
import com.forum.lab.utils.Constants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatOrchestrator {

    private final ChatSessionService sessionService;
    private final ProductService productService;
    private final QuotaService quotaService;
    private final LlmProviderChain llmProviderChain;
    private final LabMessageRepository messageRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final ScheduledExecutorService heartbeatScheduler = Executors.newSingleThreadScheduledExecutor();

    public SseEmitter handleStreamChat(String requestedSessionId, String userPrompt, String currentUsername, String clientIp) {
        // SSE timeout 2 phút (120_000ms)
        SseEmitter emitter = new SseEmitter(120_000L);

        boolean isRegistered = StringUtils.hasText(currentUsername);
        String ownerUsername = isRegistered ? currentUsername : (Constants.GUEST_USERNAME_PREFIX + clientIp.replace(":", "_"));
        String quotaKey = isRegistered ? currentUsername : clientIp;

        // 1. Kiểm tra hạn mức
        if (!quotaService.checkAndIncrement(quotaKey, isRegistered)) {
            sendEvent(emitter, Constants.SSE_EVENT_ERROR, "Bạn đã sử dụng hết hạn mức chat hôm nay. Vui lòng quay lại vào ngày mai hoặc đăng nhập tài khoản chính thức!");
            emitter.complete();
            return emitter;
        }

        // 2. Lấy hoặc tạo phiên
        LabChatSession session = sessionService.getOrCreateSession(requestedSessionId, ownerUsername);
        sendEvent(emitter, Constants.SSE_EVENT_SESSION, session.getSessionId());

        // 3. Lưu tin nhắn của User
        LabMessage userMessage = LabMessage.builder()
                .sessionId(session.getSessionId())
                .role(Constants.MESSAGE_ROLE_USER)
                .content(userPrompt)
                .createdAt(Instant.now())
                .build();
        messageRepository.save(userMessage);

        // 4. Lên lịch Heartbeat 15s để chống Cloudflare timeout 524
        ScheduledFuture<?> heartbeatTask = heartbeatScheduler.scheduleAtFixedRate(() -> {
            try {
                emitter.send(SseEmitter.event().name(Constants.SSE_EVENT_HEARTBEAT).data("ping"));
            } catch (Exception e) {
                // Emitter đã đóng
            }
        }, 15, 15, TimeUnit.SECONDS);

        emitter.onCompletion(() -> heartbeatTask.cancel(true));
        emitter.onTimeout(() -> heartbeatTask.cancel(true));
        emitter.onError(t -> heartbeatTask.cancel(true));

        // 5. Chuẩn bị lịch sử trò chuyện cho LLM
        List<LabMessage> history = messageRepository.findBySessionIdOrderByCreatedAtAsc(session.getSessionId());
        List<LlmMessage> llmMessages = new ArrayList<>();

        llmMessages.add(LlmMessage.builder()
                .role(Constants.MESSAGE_ROLE_SYSTEM)
                .content("Bạn là " + Constants.BOT_NAME + " 🧙‍♂️, trợ lý thông thái của Phòng thí nghiệm diễn đàn. " +
                        "Hãy nói chuyện thân thiện, lịch thiệp, thông thái bằng tiếng Việt. " +
                        "Bạn có khả năng tạo ra các sản phẩm như: Vòng quay may mắn, Thẻ ghi nhớ (Flashcard), và Đấu trường Ai là triệu phú.")
                .build());

        for (LabMessage msg : history) {
            llmMessages.add(LlmMessage.builder()
                    .role(msg.getRole())
                    .content(msg.getContent())
                    .build());
        }

        // 6. Phát hiện ý định tạo sản phẩm từ prompt
        String detectedTemplate = detectTemplateIntent(userPrompt);
        ProductDTO createdProduct = null;
        if (detectedTemplate != null) {
            try {
                createdProduct = productService.createProductFromTemplate(
                        session.getSessionId(),
                        ownerUsername,
                        detectedTemplate,
                        extractProductTitle(userPrompt, detectedTemplate)
                );
            } catch (Exception e) {
                log.error("Lỗi khi tự động tạo sản phẩm từ template: {}", e.getMessage());
            }
        }

        final ProductDTO finalProduct = createdProduct;
        StringBuilder assistantReply = new StringBuilder();

        // 7. Stream text từ LLM Chain
        llmProviderChain.streamChat(llmMessages)
                .subscribe(
                        chunk -> {
                            assistantReply.append(chunk);
                            sendEvent(emitter, Constants.SSE_EVENT_MESSAGE, chunk);
                        },
                        error -> {
                            log.error("Lỗi luồng stream AI: {}", error.getMessage());
                            sendEvent(emitter, Constants.SSE_EVENT_ERROR, "Đã có gián đoạn đường truyền AI. Ta vẫn lưu giữ phiên cho bạn!");
                            emitter.complete();
                        },
                        () -> {
                            // Khi stream text xong:
                            LabMessage.ProductCard productCard = null;
                            if (finalProduct != null) {
                                productCard = LabMessage.ProductCard.builder()
                                        .publicId(finalProduct.getPublicId())
                                        .title(finalProduct.getTitle())
                                        .templateType(finalProduct.getTemplateType())
                                        .build();

                                try {
                                    sendEvent(emitter, Constants.SSE_EVENT_PRODUCT, objectMapper.writeValueAsString(productCard));
                                } catch (Exception e) {
                                    log.error("Lỗi serialize productCard: {}", e.getMessage());
                                }
                            }

                            // Lưu câu trả lời của AI vào DB
                            LabMessage assistantMsg = LabMessage.builder()
                                    .sessionId(session.getSessionId())
                                    .role(Constants.MESSAGE_ROLE_ASSISTANT)
                                    .content(assistantReply.toString())
                                    .productCard(productCard)
                                    .createdAt(Instant.now())
                                    .build();
                            messageRepository.save(assistantMsg);

                            sendEvent(emitter, Constants.SSE_EVENT_DONE, "OK");
                            emitter.complete();
                        }
                );

        return emitter;
    }

    private void sendEvent(SseEmitter emitter, String eventName, String data) {
        try {
            emitter.send(SseEmitter.event().name(eventName).data(data));
        } catch (IOException e) {
            log.debug("Không thể gửi SSE event {}: {}", eventName, e.getMessage());
        }
    }

    private String detectTemplateIntent(String prompt) {
        String p = prompt.toLowerCase();
        if (p.contains("vòng quay") || p.contains("quay số") || p.contains("bốc thăm") || p.contains("may mắn") || p.contains("wheel")) {
            return Constants.TEMPLATE_LUCKY_WHEEL;
        }
        if (p.contains("thẻ") || p.contains("flashcard") || p.contains("từ vựng") || p.contains("ghi nhớ") || p.contains("lật thẻ")) {
            return Constants.TEMPLATE_FLASHCARD;
        }
        if (p.contains("triệu phú") || p.contains("millionaire") || p.contains("ai là triệu phú") || p.contains("trắc nghiệm") || p.contains("gameshow")) {
            return Constants.TEMPLATE_MILLIONAIRE;
        }
        return null;
    }

    private String extractProductTitle(String prompt, String templateType) {
        if (Constants.TEMPLATE_LUCKY_WHEEL.equals(templateType)) {
            return "Vòng quay may mắn: " + (prompt.length() > 30 ? prompt.substring(0, 30) + "..." : prompt);
        } else if (Constants.TEMPLATE_FLASHCARD.equals(templateType)) {
            return "Bộ thẻ: " + (prompt.length() > 30 ? prompt.substring(0, 30) + "..." : prompt);
        } else if (Constants.TEMPLATE_MILLIONAIRE.equals(templateType)) {
            return "Đấu trường Triệu Phú: " + (prompt.length() > 30 ? prompt.substring(0, 30) + "..." : prompt);
        }
        return "Sản phẩm tương tác";
    }
}
