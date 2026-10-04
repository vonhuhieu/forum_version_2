package com.forum.lab.controller;

import com.forum.lab.dto.MessageDTO;
import com.forum.lab.dto.ResponseDTO;
import com.forum.lab.dto.SessionDTO;
import com.forum.lab.service.ChatOrchestrator;
import com.forum.lab.service.ChatSessionService;
import com.forum.lab.utils.Constants;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

@RestController
@RequestMapping("/api/lab")
@RequiredArgsConstructor
public class ChatController {

    private final ChatOrchestrator chatOrchestrator;
    private final ChatSessionService chatSessionService;

    @GetMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamChat(
            @RequestParam(required = false) String sessionId,
            @RequestParam String message,
            Authentication authentication,
            HttpServletRequest request
    ) {
        String currentUsername = (authentication != null && authentication.isAuthenticated()) ? authentication.getName() : null;
        String clientIp = extractClientIp(request);

        return chatOrchestrator.handleStreamChat(sessionId, message, currentUsername, clientIp);
    }

    @GetMapping("/sessions")
    @PreAuthorize(Constants.PRE_AUTH_USER_OR_ADMIN)
    public ResponseEntity<ResponseDTO<Page<SessionDTO>>> getUserSessions(
            Authentication authentication,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return ResponseEntity.ok(ResponseDTO.success(chatSessionService.getUserSessions(authentication.getName(), pageable)));
    }

    @GetMapping("/sessions/{sessionId}/messages")
    public ResponseEntity<ResponseDTO<List<MessageDTO>>> getSessionMessages(
            @PathVariable String sessionId,
            Authentication authentication
    ) {
        String currentUsername = (authentication != null && authentication.isAuthenticated()) ? authentication.getName() : null;
        return ResponseEntity.ok(ResponseDTO.success(chatSessionService.getSessionMessages(sessionId, currentUsername)));
    }

    @DeleteMapping("/sessions/{sessionId}")
    @PreAuthorize(Constants.PRE_AUTH_USER_OR_ADMIN)
    public ResponseEntity<ResponseDTO<Void>> deleteSession(
            @PathVariable String sessionId,
            Authentication authentication
    ) {
        chatSessionService.deleteSession(sessionId, authentication.getName());
        return ResponseEntity.ok(ResponseDTO.success(null));
    }

    private String extractClientIp(HttpServletRequest request) {
        String cfIp = request.getHeader("CF-Connecting-IP");
        if (cfIp != null && !cfIp.isBlank()) {
            return cfIp.trim();
        }
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
