package com.forum.lab.repository;

import com.forum.lab.document.LabChatSession;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface LabChatSessionRepository extends MongoRepository<LabChatSession, String> {
    Optional<LabChatSession> findBySessionId(String sessionId);
    Page<LabChatSession> findByOwnerUsername(String ownerUsername, Pageable pageable);
    void deleteBySessionId(String sessionId);
}
