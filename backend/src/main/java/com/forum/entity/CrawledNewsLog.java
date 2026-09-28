package com.forum.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "crawled_news_log", indexes = {
    @Index(name = "idx_crawled_news_url", columnList = "source_url", unique = true)
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CrawledNewsLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "source_url", length = 500, nullable = false, unique = true)
    private String sourceUrl;

    @Column(name = "source_title", length = 500)
    private String sourceTitle;

    @Column(name = "source_name", length = 100)
    private String sourceName;

    @Column(name = "category_id")
    private Long categoryId;

    @Column(name = "thread_id")
    private Long threadId;

    @CreationTimestamp
    @Column(name = "crawled_at")
    private LocalDateTime crawledAt;
}
