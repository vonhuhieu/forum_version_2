package com.forum.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ReportDTO {
    private Long id;
    private String reason;
    private String targetType;
    private Long targetId;
    private Long reporterId;
    private String reporterUsername;
    private Long threadId; // ID của thread chứa bài viết/bình luận bị báo cáo
    private String targetAuthorUsername; // Tác giả của bài viết/bình luận bị báo cáo
    private String targetContentSnippet; // Một đoạn nội dung ngắn bị báo cáo
    private String status;
    private LocalDateTime createdAt;
    private String resolvedByUsername;
    private LocalDateTime resolvedAt;
}
