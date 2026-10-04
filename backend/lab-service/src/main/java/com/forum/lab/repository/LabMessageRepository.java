package com.forum.lab.repository;

import com.forum.lab.document.LabMessage;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LabMessageRepository extends MongoRepository<LabMessage, String> {
    List<LabMessage> findBySessionIdOrderByCreatedAtAsc(String sessionId);
    void deleteBySessionId(String sessionId);
}
