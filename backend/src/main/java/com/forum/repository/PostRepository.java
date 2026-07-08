package com.forum.repository;

import com.forum.entity.Post;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;

@Repository
public interface PostRepository extends JpaRepository<Post, Long> {
    // FIX HHH90003004: Tách JOIN FETCH collection (author.roles) ra khỏi paged query.
    // Dùng @EntityGraph chỉ fetch các quan hệ many-to-one (author) để tránh in-memory pagination.
    // Roles sẽ được load lazy theo batch (hibernate.default_batch_fetch_size=100).
    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"author"})
    @org.springframework.data.jpa.repository.Query(
        value = "SELECT p FROM Post p WHERE p.thread.id = :threadId ORDER BY p.createdAt ASC",
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

    long countByAuthorId(Long authorId);

    // FIX HHH90003004: Dùng @EntityGraph thay vì JOIN FETCH nhiều bảng + Pageable.
    // @EntityGraph với many-to-one associations không gây in-memory pagination.
    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"author", "thread", "thread.category", "thread.label"})
    @org.springframework.data.jpa.repository.Query(
        value = "SELECT p FROM Post p WHERE p.author.username = :username ORDER BY p.createdAt DESC",
        countQuery = "SELECT COUNT(p) FROM Post p WHERE p.author.username = :username"
    )
    org.springframework.data.domain.Page<Post> findByAuthorUsernameOrderByCreatedAtDesc(@org.springframework.data.repository.query.Param("username") String username, org.springframework.data.domain.Pageable pageable);
}
