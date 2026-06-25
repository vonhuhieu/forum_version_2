package com.forum.repository;

import com.forum.entity.Post;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;

@Repository
public interface PostRepository extends JpaRepository<Post, Long> {
    @org.springframework.data.jpa.repository.Query(
        value = "SELECT p FROM Post p LEFT JOIN FETCH p.author LEFT JOIN FETCH p.author.roles WHERE p.thread.id = :threadId ORDER BY p.createdAt ASC",
        countQuery = "SELECT COUNT(p) FROM Post p WHERE p.thread.id = :threadId"
    )
    org.springframework.data.domain.Page<Post> findByThreadIdOrderByCreatedAtAsc(@org.springframework.data.repository.query.Param("threadId") Long threadId, org.springframework.data.domain.Pageable pageable);

    @org.springframework.data.jpa.repository.Query("SELECT COUNT(p) FROM Post p WHERE p.thread.id = :threadId AND (p.createdAt < :createdAt OR (p.createdAt = :createdAt AND p.id < :id))")
    long countBeforePost(@org.springframework.data.repository.query.Param("threadId") Long threadId, @org.springframework.data.repository.query.Param("createdAt") java.time.LocalDateTime createdAt, @org.springframework.data.repository.query.Param("id") Long id);

    long countByThreadId(Long threadId);

    Page<Post> findByThreadId(Long threadId, Pageable pageable);
    java.util.Optional<Post> findFirstByThreadIdOrderByCreatedAtDesc(Long threadId);
    
    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("DELETE FROM Post p WHERE p.thread.id = :threadId")
    void deleteByThreadId(@org.springframework.data.repository.query.Param("threadId") Long threadId);

    @org.springframework.data.jpa.repository.Query(
        "SELECT p.thread.id, p.id, p.createdAt, u.id, u.username, u.displayName, u.email, u.avatar " +
        "FROM Post p " +
        "LEFT JOIN p.author u " +
        "WHERE p.id IN (SELECT MAX(p2.id) FROM Post p2 WHERE p2.thread.id IN :threadIds GROUP BY p2.thread.id)"
    )
    List<Object[]> findLatestPostFieldsForThreadIds(@org.springframework.data.repository.query.Param("threadIds") List<Long> threadIds);
}
