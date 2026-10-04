package com.forum.service.news;

import com.forum.elasticsearch.document.SearchDocument;
import com.forum.elasticsearch.repository.SearchDocumentRepository;
import com.forum.entity.*;
import com.forum.entity.Thread;
import com.forum.repository.*;
import com.forum.service.SystemSettingService;
import com.forum.service.ThreadService;
import com.forum.utils.Constants;
import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class NewsCuratorService {

    private final CrawledNewsLogRepository crawledNewsLogRepository;
    private final ThreadRepository threadRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final LabelRepository labelRepository;
    private final SystemSettingService systemSettingService;
    private final SearchDocumentRepository searchDocumentRepository;

    public record NewsSource(String name, String rssUrl, Long defaultCategoryId) {}

    private static final List<NewsSource> DEFAULT_SOURCES = List.of(
            // 1. Công nghệ & Lập trình & AI (Category 36: AI & Lập Trình, 35: Thiết Bị Số)
            new NewsSource("VnExpress Số Hóa", "https://vnexpress.net/rss/so-hoa.rss", Constants.CATEGORY_AI_PROGRAMMING_ID),
            new NewsSource("Dân Trí Sức Mạnh Số", "https://dantri.com.vn/rss/suc-manh-so.rss", Constants.CATEGORY_DIGITAL_DEVICES_ID),
            new NewsSource("VietNamNet Công Nghệ", "https://vietnamnet.vn/rss/cong-nghe.rss", Constants.CATEGORY_AI_PROGRAMMING_ID),
            new NewsSource("Tinh Tế", "https://tinhte.vn/rss", Constants.CATEGORY_DIGITAL_DEVICES_ID),
            new NewsSource("GenK", "https://genk.vn/rss/tin-ict.rss", Constants.CATEGORY_AI_PROGRAMMING_ID),

            // 2. Kinh tế & Tài chính & Đầu tư (Category 32: Đầu Tư & Dòng Tiền)
            new NewsSource("VnExpress Kinh Doanh", "https://vnexpress.net/rss/kinh-doanh.rss", Constants.CATEGORY_INVESTMENT_FINANCE_ID),
            new NewsSource("Tuổi Trẻ Kinh Doanh", "https://tuoitre.vn/rss/kinh-doanh.rss", Constants.CATEGORY_INVESTMENT_FINANCE_ID),

            // 3. Đời sống & Xã hội văn minh (Category 29: Trà Đá Vỉa Hè)
            new NewsSource("Tuổi Trẻ Nhịp Sống Trẻ", "https://tuoitre.vn/rss/nhip-song-tre.rss", Constants.CATEGORY_SIDEWALK_TEA_ID),
            new NewsSource("VnExpress Đời Sống", "https://vnexpress.net/rss/doi-song.rss", Constants.CATEGORY_SIDEWALK_TEA_ID)
    );

    public boolean isBotEnabled() {
        return "true".equalsIgnoreCase(systemSettingService.getSetting("news_bot_enabled", "true"));
    }

    public int getMaxDailyPosts() {
        try {
            return Integer.parseInt(systemSettingService.getSetting("news_bot_max_daily_posts", "8"));
        } catch (NumberFormatException e) {
            return 8;
        }
    }

    /**
     * Tiến hành cào tin tự động từ các nguồn và đăng bài
     */
    @Transactional
    public int fetchAndPublishNews() {
        if (!isBotEnabled()) {
            log.info("NewsBot đang ở trạng thái TẮT theo cấu hình system_settings.");
            return 0;
        }

        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        long todayCount = crawledNewsLogRepository.countByCrawledAtAfter(startOfDay);
        int maxDaily = getMaxDailyPosts();
        if (todayCount >= maxDaily) {
            log.info("Đã đạt giới hạn đăng tin trong ngày ({} / {}). Bỏ qua lượt cào này.", todayCount, maxDaily);
            return 0;
        }

        User botUser = userRepository.findByUsername(Constants.NEWS_BOT_USERNAME).orElse(null);
        if (botUser == null) {
            log.warn("Không tìm thấy user bot '{}'. Hủy lượt cào tin.", Constants.NEWS_BOT_USERNAME);
            return 0;
        }

        Label newsLabel = labelRepository.findByName(Constants.NEWS_LABEL_NAME).orElse(null);

        int publishedCount = 0;
        // Xáo trộn thứ tự nguồn để tin tức phong phú, không bị dồn một báo
        List<NewsSource> sources = new ArrayList<>(DEFAULT_SOURCES);
        Collections.shuffle(sources);

        for (NewsSource source : sources) {
            if (todayCount + publishedCount >= maxDaily) {
                break;
            }

            try {
                int countFromSource = crawlSingleSource(source, botUser, newsLabel);
                publishedCount += countFromSource;
                if (countFromSource > 0) {
                    // Dừng một chút giữa các nguồn
                    java.lang.Thread.sleep(1000);
                }
            } catch (Exception e) {
                log.error("Lỗi khi cào tin từ nguồn {}: {}", source.name(), e.getMessage());
            }
        }

        if (publishedCount > 0) {
            ThreadService.clearAllCaches();
            log.info("Đã hoàn tất phiên cào tin. Đăng thành công {} bài viết mới.", publishedCount);
        }
        return publishedCount;
    }

    private int crawlSingleSource(NewsSource source, User botUser, Label newsLabel) {
        try {
            URL feedUrl = URI.create(source.rssUrl()).toURL();
            SyndFeedInput input = new SyndFeedInput();
            SyndFeed feed = input.build(new XmlReader(feedUrl));

            if (feed == null || feed.getEntries() == null) {
                return 0;
            }

            for (SyndEntry entry : feed.getEntries()) {
                String link = entry.getLink();
                if (link == null || link.trim().isEmpty()) {
                    continue;
                }
                link = link.trim();

                // Kiểm tra trùng lặp
                if (crawledNewsLogRepository.existsBySourceUrl(link)) {
                    continue;
                }

                String title = cleanText(entry.getTitle());
                if (title == null || title.isEmpty()) {
                    continue;
                }

                // Lấy metadata chi tiết (Ảnh thumbnail, Tóm tắt)
                ArticleMetadata meta = extractArticleMetadata(link, entry);

                // Tạo bài viết chuẩn mực
                Thread savedThread = createNewsThread(source, title, link, meta, botUser, newsLabel);
                if (savedThread != null) {
                    // Lưu log tránh trùng
                    CrawledNewsLog logEntry = new CrawledNewsLog();
                    logEntry.setSourceUrl(link);
                    logEntry.setSourceTitle(title);
                    logEntry.setSourceName(source.name());
                    logEntry.setCategoryId(savedThread.getCategory() != null ? savedThread.getCategory().getId() : null);
                    logEntry.setThreadId(savedThread.getId());
                    logEntry.setCrawledAt(LocalDateTime.now());
                    crawledNewsLogRepository.save(logEntry);

                    // Cập nhật tìm kiếm OpenSearch
                    try {
                        SearchDocument doc = new SearchDocument();
                        doc.setId("thread_" + savedThread.getId());
                        doc.setOriginalId(savedThread.getId());
                        doc.setType("thread");
                        doc.setThreadId(savedThread.getId());
                        doc.setThreadTitle(savedThread.getTitle());
                        doc.setContent(savedThread.getContent());
                        doc.setCategoryName(savedThread.getCategory() != null ? savedThread.getCategory().getName() : null);
                        doc.setCategoryId(savedThread.getCategory() != null ? savedThread.getCategory().getId() : null);
                        doc.setAuthorName("Điểm Tin Bot 🤖");
                        doc.setCreatedAt(savedThread.getCreatedAt() != null ? savedThread.getCreatedAt() : LocalDateTime.now());
                        doc.setScope(savedThread.getScope());
                        doc.setActive(savedThread.isActive());
                        searchDocumentRepository.save(doc);
                    } catch (Exception ignored) {}

                    log.info("Đã đăng thành công bản tin: [{}] {}", source.name(), title);
                    return 1; // Mỗi nguồn chỉ lấy 1 bài tin mới nhất cho mỗi lượt quét
                }
            }
        } catch (Exception e) {
            log.warn("Không thể tải RSS feed {}: {}", source.rssUrl(), e.getMessage());
        }
        return 0;
    }

    private static class ArticleMetadata {
        String imageUrl;
        String excerpt;

        ArticleMetadata(String imageUrl, String excerpt) {
            this.imageUrl = imageUrl;
            this.excerpt = excerpt;
        }
    }

    private ArticleMetadata extractArticleMetadata(String articleUrl, SyndEntry entry) {
        String imageUrl = null;
        String excerpt = null;

        // 1. Thử lấy từ RSS Description trước
        if (entry.getDescription() != null && entry.getDescription().getValue() != null) {
            String descHtml = entry.getDescription().getValue();
            try {
                Document doc = Jsoup.parse(descHtml);
                Element img = doc.selectFirst("img");
                if (img != null) {
                    imageUrl = img.attr("src");
                    if (imageUrl == null || imageUrl.isEmpty()) {
                        imageUrl = img.attr("data-original");
                    }
                }
                excerpt = doc.text();
            } catch (Exception ignored) {}
        }

        // 2. Thử truy cập HTML trang báo để bóc OpenGraph tags nếu chưa có ảnh
        if (imageUrl == null || imageUrl.trim().isEmpty() || excerpt == null || excerpt.length() < 20) {
            try {
                Document pageDoc = Jsoup.connect(articleUrl)
                        .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                        .timeout(4000)
                        .get();

                if (imageUrl == null || imageUrl.trim().isEmpty()) {
                    Element ogImage = pageDoc.selectFirst("meta[property=og:image]");
                    if (ogImage != null) {
                        imageUrl = ogImage.attr("content");
                    }
                }

                if (excerpt == null || excerpt.length() < 20) {
                    Element ogDesc = pageDoc.selectFirst("meta[property=og:description]");
                    if (ogDesc != null) {
                        excerpt = ogDesc.attr("content");
                    }
                }
            } catch (Exception ignored) {}
        }

        if (excerpt != null && excerpt.length() > 300) {
            excerpt = excerpt.substring(0, 297) + "...";
        }

        return new ArticleMetadata(imageUrl, excerpt != null ? excerpt : "");
    }

    private Thread createNewsThread(NewsSource source, String title, String articleUrl, ArticleMetadata meta, User botUser, Label newsLabel) {
        Category category = categoryRepository.findById(source.defaultCategoryId())
                .or(() -> categoryRepository.findById(29L)) // Fallback sang Trà Đá Vỉa Hè
                .orElse(null);

        Thread thread = new Thread();
        thread.setTitle("[" + source.name() + "] " + title);
        thread.setCategory(category);
        thread.setLabel(newsLabel);
        thread.setAuthor(botUser);
        thread.setScope(Constants.THREAD_SCOPE_PUBLIC);
        thread.setCreatedAt(LocalDateTime.now());
        thread.setLastPostAt(LocalDateTime.now());
        thread.setViewCount(1);
        thread.setReplyCount(0);
        thread.setActive(true);
        thread.setPinned(false);
        thread.setLocked(false);

        // Sinh HTML nội dung chuẩn mực an toàn
        String htmlContent = buildSafeNewsHtml(source.name(), title, articleUrl, meta);
        thread.setContent(htmlContent);

        // Tạo khảo sát (Poll) tự động kích thích tương tác
        Poll poll = new Poll();
        poll.setThread(thread);
        poll.setQuestion("Góc nhìn của bạn về thông tin này?");
        poll.setMaxChoices(1);
        poll.setAllowChangeVote(true);
        poll.setShowResultWithoutVote(true);

        List<PollOption> options = new ArrayList<>();
        options.add(new PollOption(null, poll, "1. Đồng tình / Tích cực 👍", 0));
        options.add(new PollOption(null, poll, "2. Chưa đồng tình / Quan ngại 🤔", 0));
        options.add(new PollOption(null, poll, "3. Cần thêm thời gian theo dõi ⏳", 0));
        poll.setOptions(options);

        thread.setPoll(poll);

        return threadRepository.save(thread);
    }

    private String buildSafeNewsHtml(String sourceName, String title, String articleUrl, ArticleMetadata meta) {
        StringBuilder sb = new StringBuilder();
        sb.append("<div class=\"news-curator-card\" style=\"padding: 16px; border: 1px solid #e2e8f0; border-radius: 10px; background-color: #f8fafc; font-family: inherit; line-height: 1.6;\">\n");

        if (meta.imageUrl != null && !meta.imageUrl.trim().isEmpty()) {
            sb.append("  <div style=\"text-align: center; margin-bottom: 16px;\">\n");
            sb.append("    <img src=\"").append(meta.imageUrl).append("\" alt=\"").append(escapeHtml(title))
                    .append("\" style=\"max-width: 100%; max-height: 420px; border-radius: 8px; object-fit: cover; box-shadow: 0 4px 6px -1px rgba(0,0,0,0.1);\" />\n");
            sb.append("  </div>\n");
        }

        if (meta.excerpt != null && !meta.excerpt.trim().isEmpty()) {
            sb.append("  <div style=\"font-size: 1.05rem; color: #334155; margin-bottom: 16px; font-weight: 500;\">\n");
            sb.append("    <p style=\"margin: 0;\">").append(escapeHtml(meta.excerpt)).append("</p>\n");
            sb.append("  </div>\n");
        }

        sb.append("  <div style=\"background: #ffffff; border-left: 4px solid #1a73e8; padding: 12px 16px; border-radius: 4px; margin-bottom: 20px; display: flex; align-items: center; justify-content: space-between; flex-wrap: wrap; gap: 10px;\">\n");
        sb.append("    <div style=\"font-size: 0.92rem; color: #64748b;\">\n");
        sb.append("      <span>Nguồn phát hành: <strong style=\"color: #0f172a;\">").append(sourceName).append("</strong></span>\n");
        sb.append("    </div>\n");
        sb.append("    <div>\n");
        sb.append("      <a href=\"").append(articleUrl).append("\" target=\"_blank\" rel=\"noopener noreferrer nofollow\" style=\"display: inline-flex; align-items: center; gap: 6px; background-color: #1a73e8; color: #ffffff; text-decoration: none; padding: 8px 16px; border-radius: 6px; font-weight: 600; font-size: 0.9rem;\">\n");
        sb.append("        <span>Đọc toàn bộ bài viết gốc</span> ↗\n");
        sb.append("      </a>\n");
        sb.append("    </div>\n");
        sb.append("  </div>\n");

        sb.append("  <div style=\"padding-top: 12px; border-top: 1px dashed #cbd5e1; color: #475569; font-size: 0.95rem;\">\n");
        sb.append("    <p style=\"margin: 0;\">💬 <strong>Góc đàm đạo:</strong> Anh em nghĩ sao về diễn biến này? Hãy để lại ý kiến văn minh bên dưới nhé!</p>\n");
        sb.append("  </div>\n");
        sb.append("</div>");

        return sb.toString();
    }

    private String cleanText(String text) {
        if (text == null) return null;
        return text.trim().replaceAll("\\s+", " ");
    }

    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
