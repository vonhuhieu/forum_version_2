package com.forum.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BookmarkDTO {
    private Long id;
    private Long threadId;
    private String threadTitle;
    private Long postId;
    private String note;                // Nội dung ghi chú người dùng nhập
    private List<String> labels;        // Danh sách labels
    private String contentPreview;      // Nội dung cắt ngắn (từ note hoặc thread/post content)

    // Thông tin tác giả của thread/post được bookmark
    private Long authorId;
    private String authorUsername;
    private String authorDisplayName;
    private String authorAvatar;
    private Boolean authorIsVerifiedBadge;

    // Loại bookmark: "THREAD" hoặc "POST"
    private String type;

    private LocalDateTime createdAt;    // Thời gian add bookmark
}
