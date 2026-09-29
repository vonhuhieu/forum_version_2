package com.forum.controller;

import com.forum.dto.PageResponseDTO;
import com.forum.dto.ResponseDTO;
import com.forum.dto.ThreadDTO;
import com.forum.service.news.NewsService;
import com.forum.utils.Constants;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/news")
@RequiredArgsConstructor
public class NewsController {

    private final NewsService newsService;

    /**
     * Lấy danh sách bài viết điểm tin có phân trang và bộ lọc theo nhóm lĩnh vực
     */
    @GetMapping("/threads")
    public ResponseEntity<ResponseDTO<PageResponseDTO<ThreadDTO>>> getNewsThreads(
            @RequestParam(required = false, defaultValue = Constants.NEWS_TAB_ALL) String tab,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size) {
        return ResponseEntity.ok(newsService.getNewsThreads(tab, categoryId, keyword, page, size));
    }

    /**
     * Kích hoạt cào tin tức tức thì (Dành cho Quản trị viên)
     */
    @PostMapping("/crawl-now")
    @PreAuthorize(Constants.PRE_AUTH_ADMIN_OR_SUPER_ADMIN)
    public ResponseEntity<ResponseDTO<Map<String, Object>>> triggerManualCrawl() {
        return ResponseEntity.ok(newsService.triggerManualCrawl());
    }

    /**
     * Lấy số liệu thống kê bản tin
     */
    @GetMapping("/stats")
    public ResponseEntity<ResponseDTO<Map<String, Object>>> getNewsStats() {
        return ResponseEntity.ok(newsService.getNewsStats());
    }
}
