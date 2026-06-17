package com.forum.repository;

import com.forum.entity.Thread;
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

    List<Thread> findAllByCategoryIdOrderByPinnedDescLastPostAtDesc(Long categoryId);
    List<Thread> findAllByOrderByLastPostAtDesc();
    List<Thread> findTop20ByOrderByLastPostAtDesc();
    java.util.Optional<Thread> findFirstByCategoryIdOrderByLastPostAtDesc(Long categoryId);
    org.springframework.data.domain.Page<Thread> findAllByCategoryIdOrderByPinnedDescLastPostAtDesc(Long categoryId, org.springframework.data.domain.Pageable pageable);
    org.springframework.data.domain.Page<Thread> findAllByOrderByLastPostAtDesc(org.springframework.data.domain.Pageable pageable);

    @Query("SELECT t FROM Thread t WHERE (t.scope IS NULL OR t.scope <> '" + com.forum.utils.Constants.THREAD_SCOPE_INTERNAL + "') ORDER BY t.lastPostAt DESC")
    List<Thread> findAllPublicOrderByLastPostAtDesc();

    @Query("SELECT t FROM Thread t WHERE t.category.id = :categoryId AND (t.scope IS NULL OR t.scope <> '" + com.forum.utils.Constants.THREAD_SCOPE_INTERNAL + "') ORDER BY t.pinned DESC, t.lastPostAt DESC")
    List<Thread> findAllPublicByCategoryIdOrderByPinnedDescLastPostAtDesc(@org.springframework.data.repository.query.Param("categoryId") Long categoryId);

    @Query("SELECT t FROM Thread t WHERE (t.scope IS NULL OR t.scope <> '" + com.forum.utils.Constants.THREAD_SCOPE_INTERNAL + "') ORDER BY t.lastPostAt DESC")
    org.springframework.data.domain.Page<Thread> findAllPublicOrderByLastPostAtDesc(org.springframework.data.domain.Pageable pageable);

    @Query("SELECT t FROM Thread t WHERE t.category.id = :categoryId AND (t.scope IS NULL OR t.scope <> '" + com.forum.utils.Constants.THREAD_SCOPE_INTERNAL + "') ORDER BY t.pinned DESC, t.lastPostAt DESC")
    org.springframework.data.domain.Page<Thread> findAllPublicByCategoryIdOrderByPinnedDescLastPostAtDesc(@org.springframework.data.repository.query.Param("categoryId") Long categoryId, org.springframework.data.domain.Pageable pageable);
}

