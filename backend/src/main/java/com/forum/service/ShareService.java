package com.forum.service;

import com.forum.entity.Thread;
import com.forum.repository.ThreadRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class ShareService {

    private final ThreadRepository threadRepository;

    @Value("${app.frontend.url}")
    private String frontendUrl;

    private static final Pattern IMG_PATTERN = Pattern.compile("<img[^>]+src=[\"']([^\"']+)[\"']", Pattern.CASE_INSENSITIVE);
    private static final String SITE_NAME = "HỢP TÁC XÃ VUI VẺ";

    /**
     * Sinh trang HTML tĩnh chứa Open Graph meta tags phục vụ social bots (Facebook Crawler, Zalo...)
     * và tự động chuyển hướng người dùng thật về giao diện Vue SPA tương ứng.
     */
    public String generateShareHtml(Long threadId, String postId) {
        String baseFront = frontendUrl;
        if (baseFront != null && baseFront.endsWith("/")) {
            baseFront = baseFront.substring(0, baseFront.length() - 1);
        }

        Optional<Thread> threadOpt = threadRepository.findById(threadId);
        if (threadOpt.isEmpty()) {
            String fallbackUrl = baseFront != null ? baseFront + "/" : "/";
            return "<!DOCTYPE html><html><head>"
                    + "<meta http-equiv=\"refresh\" content=\"0;url=" + HtmlUtils.htmlEscape(fallbackUrl) + "\">"
                    + "<script>window.location.replace('" + HtmlUtils.htmlEscape(fallbackUrl) + "');</script>"
                    + "</head><body>Đang chuyển hướng...</body></html>";
        }

        Thread thread = threadOpt.get();
        String rawTitle = thread.getTitle() != null ? thread.getTitle() : "Bài viết";
        String displayTitle = rawTitle + " | " + SITE_NAME;

        // Trích xuất description (tối đa 200 ký tự từ nội dung thuần text bỏ html)
        String rawContent = thread.getContent() != null ? thread.getContent() : "";
        String plainText = rawContent.replaceAll("<[^>]*>", " ").replaceAll("\\s+", " ").trim();
        if (plainText.length() > 200) {
            plainText = plainText.substring(0, 200) + "...";
        }
        if (plainText.isEmpty()) {
            plainText = "Xem chi tiết bài thảo luận tại " + SITE_NAME;
        }

        // Tìm ảnh đầu tiên trong nội dung (nếu có thẻ img src="...")
        String imageUrl = "";
        Matcher matcher = IMG_PATTERN.matcher(rawContent);
        if (matcher.find()) {
            imageUrl = matcher.group(1);
        }

        // Xác định link đích trên Vue SPA
        String targetUrl;
        if (postId != null && !postId.trim().isEmpty() && !"main_thread_entry".equals(postId.trim())) {
            targetUrl = baseFront + "/thread/" + threadId + "?postId=" + postId.trim() + "#post-" + postId.trim();
        } else {
            targetUrl = baseFront + "/thread/" + threadId + "#post-main_thread_entry";
        }

        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>\n<html lang=\"vi\" prefix=\"og: http://ogp.me/ns#\">\n<head>\n");
        html.append("  <meta charset=\"UTF-8\">\n");
        html.append("  <title>").append(HtmlUtils.htmlEscape(displayTitle)).append("</title>\n");
        html.append("  <meta name=\"description\" content=\"").append(HtmlUtils.htmlEscape(plainText)).append("\">\n");

        // Open Graph Meta Tags cho Facebook, Zalo, Twitter, LinkedIn...
        html.append("  <meta property=\"og:type\" content=\"article\">\n");
        html.append("  <meta property=\"og:site_name\" content=\"").append(HtmlUtils.htmlEscape(SITE_NAME)).append("\">\n");
        html.append("  <meta property=\"og:title\" content=\"").append(HtmlUtils.htmlEscape(rawTitle)).append("\">\n");
        html.append("  <meta property=\"og:description\" content=\"").append(HtmlUtils.htmlEscape(plainText)).append("\">\n");
        html.append("  <meta property=\"og:url\" content=\"").append(HtmlUtils.htmlEscape(targetUrl)).append("\">\n");

        if (!imageUrl.isEmpty()) {
            html.append("  <meta property=\"og:image\" content=\"").append(HtmlUtils.htmlEscape(imageUrl)).append("\">\n");
            html.append("  <meta name=\"twitter:image\" content=\"").append(HtmlUtils.htmlEscape(imageUrl)).append("\">\n");
        }

        html.append("  <meta name=\"twitter:card\" content=\"summary_large_image\">\n");
        html.append("  <meta name=\"twitter:title\" content=\"").append(HtmlUtils.htmlEscape(rawTitle)).append("\">\n");
        html.append("  <meta name=\"twitter:description\" content=\"").append(HtmlUtils.htmlEscape(plainText)).append("\">\n");

        // Tự động chuyển hướng người dùng thật khi mở link vào Vue SPA
        html.append("  <meta http-equiv=\"refresh\" content=\"0;url=").append(HtmlUtils.htmlEscape(targetUrl)).append("\">\n");
        html.append("  <script type=\"text/javascript\">\n");
        html.append("    window.location.replace(\"").append(HtmlUtils.htmlEscape(targetUrl)).append("\");\n");
        html.append("  </script>\n");
        html.append("</head>\n<body>\n");
        html.append("  <p>Đang chuyển hướng đến bài viết: <a href=\"").append(HtmlUtils.htmlEscape(targetUrl)).append("\">")
                .append(HtmlUtils.htmlEscape(rawTitle)).append("</a>...</p>\n");
        html.append("</body>\n</html>");

        return html.toString();
    }
}
