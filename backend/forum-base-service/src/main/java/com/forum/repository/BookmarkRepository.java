package com.forum.repository;

import com.forum.entity.Bookmark;
import com.forum.entity.Post;
import com.forum.entity.Thread;
import com.forum.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BookmarkRepository extends JpaRepository<Bookmark, Long> {

    // Tìm bookmark bài gốc (post = null) của user cho thread
    Optional<Bookmark> findByUserAndThreadAndPostIsNull(User user, Thread thread);

    // Tìm bookmark reply post cụ thể của user
    Optional<Bookmark> findByUserAndPost(User user, Post post);

    // Kiểm tra bookmark tồn tại
    boolean existsByUserAndThreadAndPostIsNull(User user, Thread thread);
    boolean existsByUserAndPost(User user, Post post);

    // Phân trang bookmark theo user (sắp xếp mới nhất trước)
    Page<Bookmark> findByUserOrderByCreatedAtDesc(User user, Pageable pageable);

    // Phân trang bookmark theo user và label (JPQL dùng LIKE trên JSON field)
    @Query("SELECT b FROM Bookmark b WHERE b.user = :user AND b.labels LIKE %:label% ORDER BY b.createdAt DESC")
    Page<Bookmark> findByUserAndLabel(@Param("user") User user, @Param("label") String label, Pageable pageable);

    // Lấy tất cả labels (raw JSON strings) của user - dùng để parse và gom unique
    @Query(value = "SELECT b.labels FROM bookmarks b WHERE b.user_id = :userId AND b.labels IS NOT NULL AND b.labels != '[]' AND b.labels != ''", nativeQuery = true)
    List<String> findAllLabelsByUserId(@Param("userId") Long userId);

    // Batch check: tìm tất cả bookmark bài gốc của user cho danh sách threadIds
    @Query("SELECT b FROM Bookmark b WHERE b.user = :user AND b.thread.id IN :threadIds AND b.post IS NULL")
    List<Bookmark> findByUserAndThreadIdInAndPostIsNull(@Param("user") User user, @Param("threadIds") List<Long> threadIds);

    // Batch check: tìm tất cả bookmark reply post của user cho danh sách postIds
    @Query("SELECT b FROM Bookmark b WHERE b.user = :user AND b.post.id IN :postIds")
    List<Bookmark> findByUserAndPostIdIn(@Param("user") User user, @Param("postIds") List<Long> postIds);
}
