package com.forum.controller;

import com.forum.dto.ConversationCreateDTO;
import com.forum.dto.ConversationDTO;
import com.forum.dto.ConversationDetailDTO;
import com.forum.dto.ConversationMessageDTO;
import com.forum.dto.ResponseDTO;
import com.forum.dto.PageResponseDTO;
import com.forum.service.ConversationService;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/conversations")
@RequiredArgsConstructor
public class ConversationController {

    private final ConversationService conversationService;

    @PostMapping
    public ResponseEntity<ResponseDTO<ConversationDTO>> createConversation(@RequestBody ConversationCreateDTO createDTO) {
        try {
            return ResponseEntity.ok(conversationService.createConversation(createDTO));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ResponseDTO.fail(null, e.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<ResponseDTO<List<ConversationDTO>>> getMyConversations() {
        return ResponseEntity.ok(conversationService.getMyConversations());
    }

    @GetMapping("/list")
    public ResponseEntity<ResponseDTO<PageResponseDTO<ConversationDTO>>> getMyConversationsPaged(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(conversationService.getMyConversationsPaged(page, size));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<ResponseDTO<Long>> getMyUnreadCount() {
        return ResponseEntity.ok(conversationService.getMyUnreadCount());
    }

    @PutMapping("/read-all")
    public ResponseEntity<ResponseDTO<Void>> readAll() {
        return ResponseEntity.ok(conversationService.readAll());
    }

    @DeleteMapping("/clear-all")
    public ResponseEntity<ResponseDTO<Void>> clearAll() {
        return ResponseEntity.ok(conversationService.clearAll());
    }

    @PutMapping("/{id}/read")
    public ResponseEntity<ResponseDTO<Void>> markAsRead(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(conversationService.markAsRead(id));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ResponseDTO.fail(null, e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<ResponseDTO<ConversationDetailDTO>> getConversationDetail(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(conversationService.getConversationDetail(id));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ResponseDTO.fail(null, e.getMessage()));
        }
    }

    @PostMapping("/{id}/messages")
    public ResponseEntity<ResponseDTO<ConversationMessageDTO>> addMessage(@PathVariable Long id, @RequestBody Map<String, Object> payload) {
        try {
            String content = (String) payload.get("content");
            if (content == null || content.trim().isEmpty()) {
                throw new IllegalArgumentException("Nội dung tin nhắn không được để trống");
            }
            Long quotedMessageId = null;
            if (payload.containsKey("quotedMessageId") && payload.get("quotedMessageId") != null) {
                try {
                    quotedMessageId = Long.parseLong(String.valueOf(payload.get("quotedMessageId")));
                } catch (NumberFormatException e) {
                    // Ignore
                }
            }
            return ResponseEntity.ok(conversationService.addMessage(id, content, quotedMessageId));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ResponseDTO.fail(null, e.getMessage()));
        }
    }
}
