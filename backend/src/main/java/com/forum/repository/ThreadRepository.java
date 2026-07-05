package com.forum.repository;

import com.forum.entity.Thread;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ThreadRepository extends JpaRepository<Thread, Long> {
    @Query("SELECT SUM(t.replyCount) FROM Thread t")
    Long countReplies();

    @org.springframework.data.jpa.repository.Modifying
    @Query("UPDATE Thread t SET t.label = null WHERE t.label.id = :labelId")
    void removeLabelFromThreads(@org.springframework.data.repository.query.Param("labelId") Long labelId);

    @org.springframework.data.jpa.repository.Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Thread t SET t.viewCount = t.viewCount + 1 WHERE t.id = :threadId")
    void incrementViewCount(@org.springframework.data.repository.query.Param("threadId") Long threadId);

    @Query("SELECT t FROM Thread t LEFT JOIN FETCH t.author LEFT JOIN FETCH t.author.roles LEFT JOIN FETCH t.category LEFT JOIN FETCH t.label WHERE t.id = :id")
    java.util.Optional<Thread> findByIdEager(@org.springframework.data.repository.query.Param("id") Long id);

    @EntityGraph(attributePaths = {"category", "label", "author", "poll"})
    List<Thread> findAllByCategoryIdOrderByPinnedDescLastPostAtDesc(Long categoryId);

    @EntityGraph(attributePaths = {"category", "label", "author", "poll"})
    List<Thread> findAllByOrderByLastPostAtDesc();

    @EntityGraph(attributePaths = {"category", "label", "author", "poll"})
    List<Thread> findTop20ByOrderByLastPostAtDesc();

    @EntityGraph(attributePaths = {"category", "label", "author", "poll"})
    java.util.Optional<Thread> findFirstByCategoryIdOrderByLastPostAtDesc(Long categoryId);

    @EntityGraph(attributePaths = {"category", "label", "author", "poll"})
    org.springframework.data.domain.Page<Thread> findAllByCategoryIdOrderByPinnedDescLastPostAtDesc(Long categoryId, org.springframework.data.domain.Pageable pageable);

    @EntityGraph(attributePaths = {"category", "label", "author", "poll"})
    org.springframework.data.domain.Page<Thread> findAllByOrderByLastPostAtDesc(org.springframework.data.domain.Pageable pageable);

    @EntityGraph(attributePaths = {"category", "label", "author", "poll"})
    @Query("SELECT t FROM Thread t WHERE (t.scope IS NULL OR t.scope <> '" + com.forum.utils.Constants.THREAD_SCOPE_INTERNAL + "') ORDER BY t.lastPostAt DESC")
    List<Thread> findAllPublicOrderByLastPostAtDesc();

    @EntityGraph(attributePaths = {"category", "label", "author", "poll"})
    @Query("SELECT t FROM Thread t WHERE t.category.id = :categoryId AND (t.scope IS NULL OR t.scope <> '" + com.forum.utils.Constants.THREAD_SCOPE_INTERNAL + "') ORDER BY t.pinned DESC, t.lastPostAt DESC")
    List<Thread> findAllPublicByCategoryIdOrderByPinnedDescLastPostAtDesc(@org.springframework.data.repository.query.Param("categoryId") Long categoryId);

    @EntityGraph(attributePaths = {"category", "label", "author", "poll"})
    @Query("SELECT t FROM Thread t WHERE (t.scope IS NULL OR t.scope <> '" + com.forum.utils.Constants.THREAD_SCOPE_INTERNAL + "') ORDER BY t.lastPostAt DESC")
    org.springframework.data.domain.Page<Thread> findAllPublicOrderByLastPostAtDesc(org.springframework.data.domain.Pageable pageable);

    @EntityGraph(attributePaths = {"category", "label", "author", "poll"})
    @Query("SELECT t FROM Thread t WHERE t.category.id = :categoryId AND (t.scope IS NULL OR t.scope <> '" + com.forum.utils.Constants.THREAD_SCOPE_INTERNAL + "') ORDER BY t.lastPostAt DESC")
    List<Thread> findFirstPublicByCategoryIdOrderByLastPostAtDesc(@org.springframework.data.repository.query.Param("categoryId") Long categoryId, org.springframework.data.domain.Pageable pageable);

    @EntityGraph(attributePaths = {"category", "label", "author", "poll"})
    @Query("SELECT t FROM Thread t WHERE " +
           "(:canSeeInternal = true OR t.scope IS NULL OR t.scope <> '" + com.forum.utils.Constants.THREAD_SCOPE_INTERNAL + "') " +
           "AND (:categoryId IS NULL OR t.category.id = :categoryId) " +
           "AND (:labelId IS NULL OR t.label.id = :labelId) " +
           "AND (LOWER(t.title) LIKE :keyword " +
           "OR LOWER(t.author.username) LIKE :keyword " +
           "OR LOWER(t.author.displayName) LIKE :keyword " +
           "OR LOWER(t.category.name) LIKE :keyword)")
    org.springframework.data.domain.Page<Thread> searchThreads(
            @org.springframework.data.repository.query.Param("canSeeInternal") boolean canSeeInternal,
            @org.springframework.data.repository.query.Param("categoryId") Long categoryId, 
            @org.springframework.data.repository.query.Param("labelId") Long labelId, 
            @org.springframework.data.repository.query.Param("keyword") String keyword, 
            org.springframework.data.domain.Pageable pageable);

    @Query("SELECT t.category.id, COUNT(t), COALESCE(SUM(t.replyCount + 1), 0) FROM Thread t GROUP BY t.category.id")
    List<Object[]> getCategoryStats();

    long countByAuthorId(Long authorId);

    @EntityGraph(attributePaths = {"category", "label", "author", "poll"})
    org.springframework.data.domain.Page<Thread> findByAuthorUsernameOrderByCreatedAtDesc(String username, org.springframework.data.domain.Pageable pageable);
}


