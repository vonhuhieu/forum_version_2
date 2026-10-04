package com.forum.repository;

import com.forum.entity.ConversationParticipant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConversationParticipantRepository extends JpaRepository<ConversationParticipant, Long> {
    
    Optional<ConversationParticipant> findByConversationIdAndUserUsername(Long conversationId, String username);
    
    long countByUserUsernameAndIsReadFalse(String username);
    
    List<ConversationParticipant> findByUserUsername(String username);
}
