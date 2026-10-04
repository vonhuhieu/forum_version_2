package com.forum.service.news;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class NewsScheduler {

    private final NewsCuratorService newsCuratorService;

    /**
     * Mặc định chạy vào 7h sáng, 12h trưa và 18h tối mỗi ngày
     */
    @Scheduled(cron = "${app.news.cron:0 0 7,12,18 * * *}")
    public void scheduledNewsCrawl() {
        log.info("Bắt đầu tiến trình định kỳ cào bản tin...");
        try {
            int count = newsCuratorService.fetchAndPublishNews();
            log.info("Tiến trình cào tin định kỳ hoàn tất. Đã đăng {} bài mới.", count);
        } catch (Exception e) {
            log.error("Lỗi trong quá trình cào tin tự động: {}", e.getMessage(), e);
        }
    }
}
