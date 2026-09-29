package com.forum.controller;

import com.forum.dto.PageResponseDTO;
import com.forum.dto.ResponseDTO;
import com.forum.dto.ThreadDTO;
import com.forum.entity.Label;
import com.forum.repository.CrawledNewsLogRepository;
import com.forum.repository.LabelRepository;
import com.forum.service.ThreadService;
import com.forum.service.news.NewsCuratorService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/news")
@RequiredArgsConstructor
public class NewsController {

    private final NewsCuratorService newsCuratorService;
    private final ThreadService threadService;
    private final com.forum.repository.ThreadRepository threadRepository;
    private final LabelRepository labelRepository;
    private final CrawledNewsLogRepository crawledNewsLogRepository;

    /**
     * Lấy danh sách bài viết điểm tin có phân trang và bộ lọc theo nhóm lĩnh vực
     */
    @GetMapping("/threads")
    public ResponseEntity<ResponseDTO<PageResponseDTO<ThreadDTO>>> getNewsThreads(
            @RequestParam(required = false, defaultValue = "all") String tab,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size) {

        Label newsLabel = labelRepository.findByName("Điểm Tin").orElse(null);
        Long labelId = newsLabel != null ? newsLabel.getId() : null;

        Long targetCategoryId = categoryId;
        if (targetCategoryId == null && tab != null && !tab.equalsIgnoreCase("all")) {
            switch (tab.toLowerCase()) {
                case "tech" -> targetCategoryId = 36L; // Trí Tuệ Nhân Tạo & Lập Trình
                case "finance" -> targetCategoryId = 32L; // Đầu Tư & Dòng Tiền
                case "society" -> targetCategoryId = 29L; // Trà Đá Vỉa Hè
            }
        }

        var result = threadService.getAllThreadsPaged(
                null,
                targetCategoryId,
                labelId,
                null,
                null,
                keyword,
                "createdAt",
                "desc",
                page,
                size
        );
        return ResponseEntity.ok(result);
    }

    /**
     * Kích hoạt cào tin tức tức thì (Dành cho Quản trị viên)
     */
    @PostMapping("/crawl-now")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_SUPER_ADMIN')")
    public ResponseEntity<ResponseDTO<Map<String, Object>>> triggerManualCrawl() {
        int published = newsCuratorService.fetchAndPublishNews();
        Map<String, Object> data = new HashMap<>();
        data.put("publishedCount", published);
        data.put("message", "Đã hoàn tất phiên cào tin. Đã đăng " + published + " bản tin mới.");
        return ResponseEntity.ok(ResponseDTO.success(data));
    }

    /**
     * Lấy số liệu thống kê bản tin
     */
    @GetMapping("/stats")
    public ResponseEntity<ResponseDTO<Map<String, Object>>> getNewsStats() {
        long totalCrawled = crawledNewsLogRepository.count();
        long todayCrawled = crawledNewsLogRepository.countByCrawledAtAfter(LocalDate.now().atStartOfDay());
        int maxDaily = newsCuratorService.getMaxDailyPosts();
        boolean enabled = newsCuratorService.isBotEnabled();

        Label newsLabel = labelRepository.findByName("Điểm Tin").orElse(null);
        Long labelId = newsLabel != null ? newsLabel.getId() : null;

        Map<String, Long> tabCounts = new HashMap<>();
        if (labelId != null) {
            tabCounts.put("all", threadRepository.countByLabelId(labelId));
            tabCounts.put("tech", threadRepository.countByLabelIdAndCategoryId(labelId, 36L));
            tabCounts.put("finance", threadRepository.countByLabelIdAndCategoryId(labelId, 32L));
            tabCounts.put("society", threadRepository.countByLabelIdAndCategoryId(labelId, 29L));
        } else {
            tabCounts.put("all", 0L);
            tabCounts.put("tech", 0L);
            tabCounts.put("finance", 0L);
            tabCounts.put("society", 0L);
        }

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalCrawled", totalCrawled);
        stats.put("todayCrawled", todayCrawled);
        stats.put("maxDaily", maxDaily);
        stats.put("enabled", enabled);
        stats.put("tabCounts", tabCounts);

        return ResponseEntity.ok(ResponseDTO.success(stats));
    }
}
