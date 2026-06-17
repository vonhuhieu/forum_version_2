package com.forum.repository;

import com.forum.entity.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, Long> {

    @Query("SELECT c FROM Conversation c JOIN c.participants p WHERE p.user.username = :username AND p.isDeleted = false ORDER BY c.updatedAt DESC")
    List<Conversation> findMyConversations(@Param("username") String username);

    @Query("SELECT c FROM Conversation c JOIN c.participants p WHERE p.user.username = :username AND p.isDeleted = false ORDER BY c.updatedAt DESC")
    org.springframework.data.domain.Page<Conversation> findMyConversationsPaged(@Param("username") String username, org.springframework.data.domain.Pageable pageable);
}
