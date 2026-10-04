package com.forum.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class SearchResultDTO {
    private String type;         // "thread" or "post"
    private Long id;             // Thread ID or Post ID
    private Long threadId;       // Parent thread ID
    private String threadTitle;  // Thread title
    private String categoryName; // Category name
    private Long categoryId;     // Category ID
    private String authorName;   // Author display name or username
    private LocalDateTime createdAt;
    private String snippet;      // Context text snippet containing keyword
    private int pageNumber;      // Page number in thread (1-based)
    private double relevanceScore; // Calculated search relevance score
    private boolean contentMatched; // true if keyword matches content
}

