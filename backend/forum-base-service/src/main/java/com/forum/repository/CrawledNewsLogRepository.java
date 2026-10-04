package com.forum.repository;

import com.forum.entity.CrawledNewsLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface CrawledNewsLogRepository extends JpaRepository<CrawledNewsLog, Long> {
    boolean existsBySourceUrl(String sourceUrl);
    Optional<CrawledNewsLog> findBySourceUrl(String sourceUrl);
    long countByCrawledAtAfter(LocalDateTime after);
}
