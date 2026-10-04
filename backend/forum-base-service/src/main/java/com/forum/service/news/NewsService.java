package com.forum.service.news;

import com.forum.dto.PageResponseDTO;
import com.forum.dto.ResponseDTO;
import com.forum.dto.ThreadDTO;
import com.forum.entity.Label;
import com.forum.repository.CrawledNewsLogRepository;
import com.forum.repository.LabelRepository;
import com.forum.repository.ThreadRepository;
import com.forum.service.ThreadService;
import com.forum.utils.Constants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class NewsService {

    private final NewsCuratorService newsCuratorService;
    private final ThreadService threadService;
    private final ThreadRepository threadRepository;
    private final LabelRepository labelRepository;
    private final CrawledNewsLogRepository crawledNewsLogRepository;

    /**
     * Lấy danh sách bài viết điểm tin có phân trang và bộ lọc theo nhóm lĩnh vực
     */
    @Transactional(readOnly = true)
    public ResponseDTO<PageResponseDTO<ThreadDTO>> getNewsThreads(
            String tab,
            Long categoryId,
            String keyword,
            int page,
            int size) {

        Label newsLabel = labelRepository.findByName(Constants.NEWS_LABEL_NAME).orElse(null);
        Long labelId = newsLabel != null ? newsLabel.getId() : null;

        Long targetCategoryId = categoryId;
        if (targetCategoryId == null && tab != null && !tab.equalsIgnoreCase(Constants.NEWS_TAB_ALL)) {
            switch (tab.toLowerCase()) {
                case Constants.NEWS_TAB_TECH -> targetCategoryId = Constants.CATEGORY_AI_PROGRAMMING_ID;
                case Constants.NEWS_TAB_FINANCE -> targetCategoryId = Constants.CATEGORY_INVESTMENT_FINANCE_ID;
                case Constants.NEWS_TAB_SOCIETY -> targetCategoryId = Constants.CATEGORY_SIDEWALK_TEA_ID;
                default -> { /* Giữ nguyên categoryId nếu không khớp tab định sẵn */ }
            }
        }

        return threadService.getAllThreadsPaged(
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
    }

    /**
     * Kích hoạt cào tin tức thủ công tức thì (Dành cho Quản trị viên)
     */
    public ResponseDTO<Map<String, Object>> triggerManualCrawl() {
        int published = newsCuratorService.fetchAndPublishNews();
        Map<String, Object> data = new HashMap<>();
        data.put("publishedCount", published);
        data.put("message", "Đã hoàn tất phiên cào tin. Đã đăng " + published + " bản tin mới.");
        return ResponseDTO.success(data);
    }

    /**
     * Lấy số liệu thống kê bản tin
     */
    @Transactional(readOnly = true)
    public ResponseDTO<Map<String, Object>> getNewsStats() {
        long totalCrawled = crawledNewsLogRepository.count();
        long todayCrawled = crawledNewsLogRepository.countByCrawledAtAfter(LocalDate.now().atStartOfDay());
        int maxDaily = newsCuratorService.getMaxDailyPosts();
        boolean enabled = newsCuratorService.isBotEnabled();

        Label newsLabel = labelRepository.findByName(Constants.NEWS_LABEL_NAME).orElse(null);
        Long labelId = newsLabel != null ? newsLabel.getId() : null;

        Map<String, Long> tabCounts = new HashMap<>();
        if (labelId != null) {
            long techCount = threadRepository.countByLabelIdAndCategoryId(labelId, Constants.CATEGORY_AI_PROGRAMMING_ID);
            long financeCount = threadRepository.countByLabelIdAndCategoryId(labelId, Constants.CATEGORY_INVESTMENT_FINANCE_ID);
            long societyCount = threadRepository.countByLabelIdAndCategoryId(labelId, Constants.CATEGORY_SIDEWALK_TEA_ID);
            tabCounts.put(Constants.NEWS_TAB_TECH, techCount);
            tabCounts.put(Constants.NEWS_TAB_FINANCE, financeCount);
            tabCounts.put(Constants.NEWS_TAB_SOCIETY, societyCount);
            tabCounts.put(Constants.NEWS_TAB_ALL, techCount + financeCount + societyCount);
        } else {
            tabCounts.put(Constants.NEWS_TAB_ALL, 0L);
            tabCounts.put(Constants.NEWS_TAB_TECH, 0L);
            tabCounts.put(Constants.NEWS_TAB_FINANCE, 0L);
            tabCounts.put(Constants.NEWS_TAB_SOCIETY, 0L);
        }

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalCrawled", totalCrawled);
        stats.put("todayCrawled", todayCrawled);
        stats.put("maxDaily", maxDaily);
        stats.put("enabled", enabled);
        stats.put("tabCounts", tabCounts);

        return ResponseDTO.success(stats);
    }
}
