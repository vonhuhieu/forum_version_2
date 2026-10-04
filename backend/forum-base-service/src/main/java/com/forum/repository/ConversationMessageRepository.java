package com.forum.repository;

import com.forum.entity.ConversationMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ConversationMessageRepository extends JpaRepository<ConversationMessage, Long> {
    List<ConversationMessage> findByConversationIdOrderByCreatedAtAsc(Long conversationId);
    java.util.Optional<ConversationMessage> findFirstByConversationIdOrderByCreatedAtAsc(Long conversationId);
    java.util.Optional<ConversationMessage> findFirstByConversationIdOrderByCreatedAtDesc(Long conversationId);
}
