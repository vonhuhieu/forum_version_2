package com.forum.lab.service;

import com.forum.lab.document.LabChatSession;
import com.forum.lab.document.LabMessage;
import com.forum.lab.dto.MessageDTO;
import com.forum.lab.dto.SessionDTO;
import com.forum.lab.repository.LabChatSessionRepository;
import com.forum.lab.repository.LabMessageRepository;
import com.forum.lab.utils.Constants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatSessionService {

    private final LabChatSessionRepository sessionRepository;
    private final LabMessageRepository messageRepository;

    public LabChatSession getOrCreateSession(String sessionId, String ownerUsername) {
        if (StringUtils.hasText(sessionId)) {
            return sessionRepository.findBySessionId(sessionId).orElseGet(() -> createNewSession(sessionId, ownerUsername));
        }
        return createNewSession(UUID.randomUUID().toString(), ownerUsername);
    }

    private LabChatSession createNewSession(String sessionId, String ownerUsername) {
        boolean isGuest = !StringUtils.hasText(ownerUsername) || ownerUsername.startsWith(Constants.GUEST_USERNAME_PREFIX);
        Instant expiresAt = isGuest ? Instant.now().plus(Duration.ofHours(24)) : null;

        LabChatSession session = LabChatSession.builder()
                .sessionId(sessionId)
                .ownerUsername(ownerUsername)
                .title("Cuộc trò chuyện mới")
                .status("ACTIVE")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .expiresAt(expiresAt)
                .build();

        return sessionRepository.save(session);
    }

    public Page<SessionDTO> getUserSessions(String ownerUsername, Pageable pageable) {
        return sessionRepository.findByOwnerUsername(ownerUsername, pageable).map(s -> SessionDTO.builder()
                .sessionId(s.getSessionId())
                .title(s.getTitle())
                .status(s.getStatus())
                .productId(s.getProductId())
                .createdAt(s.getCreatedAt())
                .updatedAt(s.getUpdatedAt())
                .build());
    }

    public List<MessageDTO> getSessionMessages(String sessionId, String currentUsername) {
        LabChatSession session = sessionRepository.findBySessionId(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phiên trò chuyện: " + sessionId));

        if (StringUtils.hasText(session.getOwnerUsername())
                && !session.getOwnerUsername().startsWith(Constants.GUEST_USERNAME_PREFIX)
                && !session.getOwnerUsername().equals(currentUsername)) {
            throw new AccessDeniedException("Bạn không có quyền xem cuộc trò chuyện này");
        }

        return messageRepository.findBySessionIdOrderByCreatedAtAsc(sessionId).stream()
                .map(m -> MessageDTO.builder()
                        .id(m.getId())
                        .sessionId(m.getSessionId())
                        .role(m.getRole())
                        .content(m.getContent())
                        .productCard(m.getProductCard())
                        .createdAt(m.getCreatedAt())
                        .build())
                .collect(Collectors.toList());
    }

    public void deleteSession(String sessionId, String currentUsername) {
        LabChatSession session = sessionRepository.findBySessionId(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phiên trò chuyện: " + sessionId));

        if (!session.getOwnerUsername().equals(currentUsername)) {
            throw new AccessDeniedException("Bạn không có quyền xoá cuộc trò chuyện này");
        }

        messageRepository.deleteBySessionId(sessionId);
        sessionRepository.deleteBySessionId(sessionId);
    }
}
