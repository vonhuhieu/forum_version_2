package com.forum.repository;

import com.forum.dto.ReportGroupDTO;
import com.forum.entity.Report;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReportRepository extends JpaRepository<Report, Long> {
    Page<Report> findByStatusOrderByCreatedAtDesc(String status, Pageable pageable);
    Page<Report> findAllByOrderByCreatedAtDesc(Pageable pageable);

    @Query("SELECT new com.forum.dto.ReportGroupDTO(r.targetType, r.targetId, r.status, COUNT(r), MAX(r.createdAt)) " +
           "FROM Report r " +
           "WHERE (:status IS NULL OR r.status = :status) " +
           "GROUP BY r.targetType, r.targetId, r.status " +
           "ORDER BY MAX(r.createdAt) DESC")
    Page<ReportGroupDTO> findGroupedReports(@Param("status") String status, Pageable pageable);

    Page<Report> findByTargetTypeAndTargetIdAndStatusOrderByCreatedAtDesc(String targetType, Long targetId, String status, Pageable pageable);
    Page<Report> findByTargetTypeAndTargetIdOrderByCreatedAtDesc(String targetType, Long targetId, Pageable pageable);

    List<Report> findByTargetTypeAndTargetIdAndStatusOrderByCreatedAtDesc(String targetType, Long targetId, String status);
}
