package com.forum.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "bookmarks", indexes = {
    @Index(name = "idx_bookmarks_user_id", columnList = "user_id"),
    @Index(name = "idx_bookmarks_user_thread", columnList = "user_id, thread_id"),
    @Index(name = "idx_bookmarks_user_post", columnList = "user_id, post_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Bookmark {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "thread_id", nullable = false)
    private Thread thread;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id")
    private Post post; // null khi bookmark bài gốc (#1), có giá trị khi bookmark reply post

    @Column(columnDefinition = "TEXT")
    private String note; // Nội dung ghi chú (field "Nội dung" trong popup)

    @Column(columnDefinition = "TEXT")
    private String labels; // Labels lưu dạng JSON array string, VD: ["test 1","test 2"]

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
