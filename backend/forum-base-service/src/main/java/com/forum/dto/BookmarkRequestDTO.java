package com.forum.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BookmarkRequestDTO {
    private Long threadId;          // Bắt buộc
    private Long postId;            // Optional (null = bookmark thread/bài gốc)
    private String note;            // Optional
    private List<String> labels;    // Optional
}
