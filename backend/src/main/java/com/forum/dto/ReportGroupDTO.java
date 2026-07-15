package com.forum.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReportGroupDTO {
    private String targetType;
    private Long targetId;
    private String status;
    private Long reportCount;
    private LocalDateTime latestReportedAt;

    // Additional transient details populated in Service
    private String targetContentSnippet;
    private String targetAuthorUsername;
    private Long threadId;

    public ReportGroupDTO(String targetType, Long targetId, String status, Long reportCount, LocalDateTime latestReportedAt) {
        this.targetType = targetType;
        this.targetId = targetId;
        this.status = status;
        this.reportCount = reportCount;
        this.latestReportedAt = latestReportedAt;
    }
}
