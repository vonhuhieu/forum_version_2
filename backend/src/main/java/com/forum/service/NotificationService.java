package com.forum.service;

import com.forum.dto.NotificationDTO;
import com.forum.dto.ResponseDTO;
import com.forum.dto.ConversationDTO;
import com.forum.entity.Notification;
import com.forum.entity.NotificationType;
import com.forum.entity.Post;
import com.forum.entity.ReactionIcon;
import com.forum.entity.Thread;
import com.forum.entity.User;
import com.forum.entity.Conversation;
import com.forum.entity.ConversationMessage;
import com.forum.mapper.NotificationMapper;
import com.forum.repository.NotificationRepository;
import com.forum.repository.UserRepository;
import com.forum.repository.ConversationRepository;
import com.forum.repository.ConversationMessageRepository;
import com.forum.repository.ReactionIconRepository;
import com.forum.repository.ThreadRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.scheduling.annotation.Async;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationMapper notificationMapper;
    private final SimpMessagingTemplate messagingTemplate;
    private final UserRepository userRepository;
    private final ConversationRepository conversationRepository;
    private final ConversationMessageRepository conversationMessageRepository;
    private final ReactionIconRepository reactionIconRepository;
    private final ThreadRepository threadRepository;

    /**
     * Logic to generate notification record and fire realtime message to targeted user socket.
     */
    @Async
    @Transactional
    public void sendNewCommentNotification(User actor, Thread thread, Post post) {
        sendNewCommentNotification(actor, thread, post, thread.getAuthor());
    }

    @Async
    @Transactional
    public void sendNewCommentNotification(User actor, Thread thread, Post post, User recipient) {
        // Don't notify user about their own actions
        if (recipient == null || actor == null || recipient.getId().equals(actor.getId())) {
            return;
        }

        User managedActor = (actor.getId() != null) ? userRepository.findById(actor.getId()).orElse(actor) : actor;
        User managedRecipient = (recipient.getId() != null) ? userRepository.findById(recipient.getId()).orElse(recipient) : recipient;
        Thread managedThread = (thread != null && thread.getId() != null) ? threadRepository.findById(thread.getId()).orElse(thread) : thread;

        Notification notif = new Notification();
        notif.setRecipient(managedRecipient);
        notif.setActor(managedActor);
        notif.setType(NotificationType.NEW_COMMENT);
        notif.setThread(managedThread);
        notif.setPost(post);
        notif.setRead(false);

        Notification saved = notificationRepository.save(notif);
        NotificationDTO dto = notificationMapper.toDTO(saved);

        pushNotification(managedRecipient.getId(), dto);
    }

    @Async
    @Transactional
    public void sendQuoteNotification(User actor, User recipient, Thread thread, Post post) {
        // Don't notify user about their own actions
        if (recipient == null || actor == null || recipient.getId().equals(actor.getId())) {
            return;
        }

        User managedActor = (actor.getId() != null) ? userRepository.findById(actor.getId()).orElse(actor) : actor;
        User managedRecipient = (recipient.getId() != null) ? userRepository.findById(recipient.getId()).orElse(recipient) : recipient;
        Thread managedThread = (thread != null && thread.getId() != null) ? threadRepository.findById(thread.getId()).orElse(thread) : thread;

        Notification notif = new Notification();
        notif.setRecipient(managedRecipient);
        notif.setActor(managedActor);
        notif.setType(NotificationType.QUOTE);
        notif.setThread(managedThread);
        notif.setPost(post);
        notif.setRead(false);

        Notification saved = notificationRepository.save(notif);
        NotificationDTO dto = notificationMapper.toDTO(saved);

        pushNotification(managedRecipient.getId(), dto);
    }

    @Async
    @Transactional
    public void sendFollowedUserThreadNotification(User actor, Thread thread, User recipient) {
        if (recipient == null || actor == null || recipient.getId().equals(actor.getId())) {
            return;
        }

        User managedActor = (actor.getId() != null) ? userRepository.findById(actor.getId()).orElse(actor) : actor;
        User managedRecipient = (recipient.getId() != null) ? userRepository.findById(recipient.getId()).orElse(recipient) : recipient;
        Thread managedThread = (thread != null && thread.getId() != null) ? threadRepository.findById(thread.getId()).orElse(thread) : thread;

        Notification notif = new Notification();
        notif.setRecipient(managedRecipient);
        notif.setActor(managedActor);
        notif.setType(NotificationType.FOLLOWED_USER_THREAD);
        notif.setThread(managedThread);
        notif.setRead(false);

        Notification saved = notificationRepository.save(notif);
        NotificationDTO dto = notificationMapper.toDTO(saved);

        pushNotification(managedRecipient.getId(), dto);
    }

    @Async
    @Transactional
    public void sendFollowedUserPostNotification(User actor, Thread thread, Post post, User recipient) {
        if (recipient == null || actor == null || recipient.getId().equals(actor.getId())) {
            return;
        }

        User managedActor = (actor.getId() != null) ? userRepository.findById(actor.getId()).orElse(actor) : actor;
        User managedRecipient = (recipient.getId() != null) ? userRepository.findById(recipient.getId()).orElse(recipient) : recipient;
        Thread managedThread = (thread != null && thread.getId() != null) ? threadRepository.findById(thread.getId()).orElse(thread) : thread;

        Notification notif = new Notification();
        notif.setRecipient(managedRecipient);
        notif.setActor(managedActor);
        notif.setType(NotificationType.FOLLOWED_USER_POST);
        notif.setThread(managedThread);
        notif.setPost(post);
        notif.setRead(false);

        Notification saved = notificationRepository.save(notif);
        NotificationDTO dto = notificationMapper.toDTO(saved);

        pushNotification(managedRecipient.getId(), dto);
    }

    @Async
    @Transactional
    public void sendReactionNotification(User actor, User recipient, Thread thread, Post post, ReactionIcon icon) {
        // Don't notify user about their own actions
        if (recipient == null || actor == null || recipient.getId().equals(actor.getId())) {
            return;
        }

        User managedActor = (actor.getId() != null) ? userRepository.findById(actor.getId()).orElse(actor) : actor;
        User managedRecipient = (recipient.getId() != null) ? userRepository.findById(recipient.getId()).orElse(recipient) : recipient;
        Thread managedThread = (thread != null && thread.getId() != null) ? threadRepository.findById(thread.getId()).orElse(thread) : thread;

        Notification notif = new Notification();
        notif.setRecipient(managedRecipient);
        notif.setActor(managedActor);
        notif.setType(NotificationType.REACTION);
        notif.setThread(managedThread);
        notif.setPost(post);
        notif.setReactionIcon(icon.getIcon());
        notif.setReactionName(icon.getTooltip());
        notif.setReactionColor(icon.getColor());
        notif.setRead(false);

        Notification saved = notificationRepository.save(notif);
        NotificationDTO dto = notificationMapper.toDTO(saved);

        pushNotification(managedRecipient.getId(), dto);
    }

    private void pushNotification(Long userId, NotificationDTO dto) {
        try {
            String dest = "/topic/notifications/" + userId;
            messagingTemplate.convertAndSend(dest, dto);
            log.info("Successfully pushed realtime notification to topic: {}", dest);
        } catch (Exception e) {
            log.warn("Failed to push realtime websocket message, but record persists in DB: {}", e.getMessage());
        }
    }

    public ResponseDTO<List<NotificationDTO>> getMyNotifications() {
        String username = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        List<Notification> list = notificationRepository.findByRecipientUsernameOrderByCreatedAtDesc(username);
        List<Notification> filtered = list.stream()
                .filter(n -> n.getType() != NotificationType.CONVERSATION_REACTION && n.getType() != NotificationType.CONVERSATION_REPLY && n.getType() != NotificationType.CONVERSATION_QUOTE && n.getType() != NotificationType.CONVERSATION_MENTION)
                .collect(java.util.stream.Collectors.toList());
        return ResponseDTO.success(notificationMapper.toDTOList(filtered));
    }

    public ResponseDTO<org.springframework.data.domain.Page<NotificationDTO>> getMyNotificationsPage(org.springframework.data.domain.Pageable pageable) {
        String username = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        org.springframework.data.domain.Page<Notification> page = notificationRepository.findByRecipientUsernameAndTypeNotIn(
            username,
            List.of(
                NotificationType.CONVERSATION_REACTION,
                NotificationType.CONVERSATION_REPLY,
                NotificationType.CONVERSATION_QUOTE,
                NotificationType.CONVERSATION_MENTION
            ),
            pageable
        );
        org.springframework.data.domain.Page<NotificationDTO> dtoPage = page.map(notificationMapper::toDTO);
        return ResponseDTO.success(dtoPage);
    }

    public ResponseDTO<Long> getMyUnreadCount() {
        String username = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        long count = notificationRepository.countByRecipientUsernameAndTypeNotInAndIsReadFalse(username, List.of(NotificationType.CONVERSATION_REACTION, NotificationType.CONVERSATION_REPLY, NotificationType.CONVERSATION_QUOTE, NotificationType.CONVERSATION_MENTION));
        return ResponseDTO.success(count);
    }

    public ResponseDTO<Void> markAsRead(Long id) {
        notificationRepository.findById(id).ifPresent(notif -> {
            notif.setRead(true);
            notificationRepository.save(notif);
        });
        return ResponseDTO.success(null);
    }

    public ResponseDTO<Void> markAllAsRead() {
        String username = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        List<Notification> unreads = notificationRepository.findByRecipientUsernameOrderByCreatedAtDesc(username);
        unreads.stream().filter(n -> !n.isRead()).forEach(n -> {
            n.setRead(true);
            notificationRepository.save(n);
        });
        return ResponseDTO.success(null);
    }

    public ResponseDTO<Void> clearAll() {
        String username = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        notificationRepository.deleteAllNonConversationNotificationsForUser(username);
        return ResponseDTO.success(null);
    }

    @Async
    @Transactional
    public void sendMentionNotification(User actor, User recipient, Thread thread, Post post) {
        // Don't notify user about their own actions
        if (recipient == null || actor == null || recipient.getId().equals(actor.getId())) {
            return;
        }

        User managedActor = (actor.getId() != null) ? userRepository.findById(actor.getId()).orElse(actor) : actor;
        User managedRecipient = (recipient.getId() != null) ? userRepository.findById(recipient.getId()).orElse(recipient) : recipient;
        Thread managedThread = (thread != null && thread.getId() != null) ? threadRepository.findById(thread.getId()).orElse(thread) : thread;

        Notification notif = new Notification();
        notif.setRecipient(managedRecipient);
        notif.setActor(managedActor);
        notif.setType(NotificationType.MENTION);
        notif.setThread(managedThread);
        notif.setPost(post);
        notif.setRead(false);

        Notification saved = notificationRepository.save(notif);
        NotificationDTO dto = notificationMapper.toDTO(saved);

        pushNotification(managedRecipient.getId(), dto);
    }

    public java.util.Set<Long> getMentionedUserIds(User actor, String content) {
        java.util.Set<Long> mentionedIds = new java.util.HashSet<>();
        if (content == null || content.isEmpty() || actor == null) {
            return mentionedIds;
        }

        // Remove HTML tags to get clean plain text
        String plainText = content.replaceAll("<[^>]*>", "");

        // Normalise plain text to replace diacritics and convert to lowercase for comparison
        String normalizedText = removeDiacritics(plainText);

        // Fetch all active users from database to compare
        List<User> allUsers = userRepository.findAll();
        for (User user : allUsers) {
            // Exclude actor
            if (user.getId().equals(actor.getId())) {
                continue;
            }

            // Formulate mention targets (display name or username)
            String displayNameTag = "@" + (user.getDisplayName() != null ? user.getDisplayName() : user.getUsername());
            String usernameTag = "@" + user.getUsername();

            // Remove diacritics from tags
            String normDisplayTag = removeDiacritics(displayNameTag);
            String normUsernameTag = removeDiacritics(usernameTag);

            // Check if normalizedText contains the tag
            if (normalizedText.contains(normDisplayTag) || normalizedText.contains(normUsernameTag)) {
                mentionedIds.add(user.getId());
            }
        }
        return mentionedIds;
    }

    @Async
    @Transactional
    public void processMentionsAsync(Long actorId, Long threadId) {
        try {
            User actor = userRepository.findById(actorId).orElse(null);
            Thread thread = threadRepository.findById(threadId).orElse(null);
            if (actor == null || thread == null) {
                return;
            }
            String content = thread.getContent();
            java.util.Set<Long> mentionedUserIds = getMentionedUserIds(actor, content);
            for (Long recipientId : mentionedUserIds) {
                userRepository.findById(recipientId).ifPresent(recipient -> {
                    sendMentionNotification(actor, recipient, thread, null);
                });
            }
        } catch (Exception e) {
            log.error("Failed to process mentions async", e);
        }
    }

    private String removeDiacritics(String str) {
        if (str == null) {
            return "";
        }
        String nfdNormalizedString = java.text.Normalizer.normalize(str, java.text.Normalizer.Form.NFD);
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
        String result = pattern.matcher(nfdNormalizedString).replaceAll("");
        result = result.replace('đ', 'd').replace('Đ', 'D');
        return result.toLowerCase();
    }

    @Async
    @Transactional
    public void sendConversationReactionNotification(Long actorId, Long recipientId, Long conversationId, Long messageId, Long iconId) {
        User actor = userRepository.findById(actorId).orElse(null);
        User recipient = userRepository.findById(recipientId).orElse(null);
        Conversation conversation = conversationRepository.findById(conversationId).orElse(null);
        ConversationMessage message = conversationMessageRepository.findById(messageId).orElse(null);
        ReactionIcon icon = reactionIconRepository.findById(iconId).orElse(null);

        if (recipient == null || actor == null || conversation == null || message == null || icon == null) {
            return;
        }

        if (recipient.getId().equals(actor.getId())) {
            return;
        }

        Notification notif = new Notification();
        notif.setRecipient(recipient);
        notif.setActor(actor);
        notif.setType(NotificationType.CONVERSATION_REACTION);
        notif.setConversation(conversation);
        notif.setConversationMessage(message);
        notif.setReactionIcon(icon.getIcon());
        notif.setReactionName(icon.getTooltip());
        notif.setReactionColor(icon.getColor());
        notif.setRead(false);

        Notification saved = notificationRepository.save(notif);
        
        ConversationDTO convoDTO = new ConversationDTO();
        convoDTO.setId(conversation.getId());
        convoDTO.setTitle(conversation.getTitle());
        convoDTO.setFirstMessageId(message.getId());
        convoDTO.setUpdatedAt(saved.getCreatedAt());
        convoDTO.setCreatorAvatar(actor.getAvatar());
        convoDTO.setCreatorUsername(actor.getUsername());
        convoDTO.setCreatorDisplayName(actor.getDisplayName());
        convoDTO.setRead(false);
        convoDTO.setReaction(true);
        convoDTO.setNotificationId(saved.getId());
        convoDTO.setReactionIcon(icon.getIcon());
        convoDTO.setReactionName(icon.getTooltip());
        convoDTO.setReactionColor(icon.getColor());

        pushConversationNotification(recipient.getId(), convoDTO);
    }

    private void pushConversationNotification(Long userId, ConversationDTO dto) {
        try {
            String dest = "/topic/conversations/" + userId;
            messagingTemplate.convertAndSend(dest, dto);
            log.info("Successfully pushed realtime conversation notification to topic: {}", dest);
        } catch (Exception e) {
            log.warn("Failed to push realtime websocket conversation notification: {}", e.getMessage());
        }
    }
}
