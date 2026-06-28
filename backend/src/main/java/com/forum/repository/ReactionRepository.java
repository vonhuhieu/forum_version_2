package com.forum.repository;

import com.forum.entity.Reaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReactionRepository extends JpaRepository<Reaction, Long> {

    Optional<Reaction> findByUserIdAndThreadId(Long userId, Long threadId);

    Optional<Reaction> findByUserIdAndPostId(Long userId, Long postId);

    void deleteByUserIdAndThreadId(Long userId, Long threadId);

    void deleteByUserIdAndPostId(Long userId, Long postId);

    @Query("SELECT r FROM Reaction r JOIN FETCH r.reactionIcon JOIN FETCH r.user JOIN FETCH r.post WHERE r.post.thread.id = :threadId")
    List<Reaction> findAllByPostThreadId(@Param("threadId") Long threadId);

    @Query("SELECT r FROM Reaction r JOIN FETCH r.user WHERE r.thread.id = :threadId ORDER BY r.updatedAt DESC")
    List<Reaction> findRecentReactorsForThread(@Param("threadId") Long threadId, org.springframework.data.domain.Pageable pageable);

    @Query("SELECT r FROM Reaction r JOIN FETCH r.reactionIcon WHERE r.user.username = :username AND r.post.thread.id = :threadId")
    List<Reaction> findAllByUsernameAndThreadId(@Param("username") String username, @Param("threadId") Long threadId);

    @Query("SELECT r FROM Reaction r WHERE r.user.username = :username AND r.thread.id = :threadId")
    Optional<Reaction> findByUsernameAndThreadId(@Param("username") String username, @Param("threadId") Long threadId);

    @Query("SELECT r FROM Reaction r WHERE r.user.username = :username AND r.post.id = :postId")
    Optional<Reaction> findByUsernameAndPostId(@Param("username") String username, @Param("postId") Long postId);

    @Query("SELECT r.reactionIcon, COUNT(r), MAX(r.updatedAt) FROM Reaction r WHERE r.thread.id = :threadId GROUP BY r.reactionIcon")
    List<Object[]> aggregateByThreadId(@Param("threadId") Long threadId);

    @Query("SELECT r.reactionIcon, COUNT(r), MAX(r.updatedAt) FROM Reaction r WHERE r.post.id = :postId GROUP BY r.reactionIcon")
    List<Object[]> aggregateByPostId(@Param("postId") Long postId);

    List<Reaction> findTop3ByThreadIdOrderByUpdatedAtDesc(Long threadId);

    List<Reaction> findTop3ByPostIdOrderByUpdatedAtDesc(Long postId);

    @org.springframework.data.jpa.repository.Modifying
    @Query("DELETE FROM Reaction r WHERE r.thread.id = :threadId OR r.post.id IN (SELECT p.id FROM Post p WHERE p.thread.id = :threadId)")
    void deleteByThreadIdOrPostThreadId(@Param("threadId") Long threadId);

    org.springframework.data.domain.Page<Reaction> findByThreadId(Long threadId, org.springframework.data.domain.Pageable pageable);

    org.springframework.data.domain.Page<Reaction> findByThreadIdAndReactionIconId(Long threadId, Long iconId, org.springframework.data.domain.Pageable pageable);

    org.springframework.data.domain.Page<Reaction> findByPostId(Long postId, org.springframework.data.domain.Pageable pageable);

    org.springframework.data.domain.Page<Reaction> findByPostIdAndReactionIconId(Long postId, Long iconId, org.springframework.data.domain.Pageable pageable);

    Optional<Reaction> findByUserIdAndConversationMessageId(Long userId, Long messageId);

    void deleteByUserIdAndConversationMessageId(Long userId, Long messageId);

    @Query("SELECT r.reactionIcon, COUNT(r), MAX(r.updatedAt) FROM Reaction r WHERE r.conversationMessage.id = :messageId GROUP BY r.reactionIcon")
    List<Object[]> aggregateByConversationMessageId(@Param("messageId") Long messageId);

    List<Reaction> findTop3ByConversationMessageIdOrderByUpdatedAtDesc(Long messageId);

    org.springframework.data.domain.Page<Reaction> findByConversationMessageId(Long messageId, org.springframework.data.domain.Pageable pageable);

    org.springframework.data.domain.Page<Reaction> findByConversationMessageIdAndReactionIconId(Long messageId, Long iconId, org.springframework.data.domain.Pageable pageable);

    @Query("SELECT COUNT(r) FROM Reaction r " +
           "LEFT JOIN r.thread t " +
           "LEFT JOIN r.post p " +
           "WHERE (t.author.id = :userId AND p.id IS NULL) OR (p.author.id = :userId)")
    long countReactionsReceivedByUserId(@Param("userId") Long userId);
}
