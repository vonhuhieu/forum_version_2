package com.forum.service;

import com.forum.entity.Thread;
import com.forum.repository.ThreadRepository;
import jakarta.servlet.http.HttpServletRequest;
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
    public String generateShareHtml(Long threadId, String postId, HttpServletRequest request) {
        String userAgent = request.getHeader("User-Agent");
        String clientIp = request.getHeader("X-Forwarded-For");
        if (clientIp == null || clientIp.isEmpty()) {
            clientIp = request.getRemoteAddr();
        }
        log.info("Social share request received: threadId={}, postId={}, IP={}, User-Agent={}", threadId, postId, clientIp, userAgent);

        String baseFront = frontendUrl;
        if (baseFront != null && baseFront.endsWith("/")) {
            baseFront = baseFront.substring(0, baseFront.length() - 1);
        }

        // URL của chính endpoint share hiện tại (được Facebook Bot cào)
        String currentShareUrl = request.getRequestURL().toString();
        if (request.getQueryString() != null && !request.getQueryString().isEmpty()) {
            currentShareUrl += "?" + request.getQueryString();
        }

        Optional<Thread> threadOpt = threadRepository.findById(threadId);
        if (threadOpt.isEmpty()) {
            String fallbackUrl = baseFront != null ? baseFront + "/" : "/";
            log.warn("Social share threadId={} not found, fallback to home: {}", threadId, fallbackUrl);
            return "<!DOCTYPE html><html><head>"
                    + "<script>window.location.replace('" + HtmlUtils.htmlEscape(fallbackUrl) + "');</script>"
                    + "<noscript><meta http-equiv=\"refresh\" content=\"0;url=" + HtmlUtils.htmlEscape(fallbackUrl) + "\"></noscript>"
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

        // Xác định link đích trên Vue SPA để chuyển hướng người dùng thật
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
        // og:url PHẢI trỏ về chính URL mà crawler đang truy cập để Facebook không cào sang trang Vue SPA
        html.append("  <meta property=\"og:url\" content=\"").append(HtmlUtils.htmlEscape(currentShareUrl)).append("\">\n");

        if (!imageUrl.isEmpty()) {
            html.append("  <meta property=\"og:image\" content=\"").append(HtmlUtils.htmlEscape(imageUrl)).append("\">\n");
            html.append("  <meta name=\"twitter:image\" content=\"").append(HtmlUtils.htmlEscape(imageUrl)).append("\">\n");
        }

        html.append("  <meta name=\"twitter:card\" content=\"summary_large_image\">\n");
        html.append("  <meta name=\"twitter:title\" content=\"").append(HtmlUtils.htmlEscape(rawTitle)).append("\">\n");
        html.append("  <meta name=\"twitter:description\" content=\"").append(HtmlUtils.htmlEscape(plainText)).append("\">\n");

        // Chuyển hướng người dùng thật bằng JavaScript.
        // Bot (Facebook, Zalo, Twitter) không chạy JavaScript nên sẽ ở lại đọc trọn vẹn thẻ Open Graph.
        html.append("  <script type=\"text/javascript\">\n");
        html.append("    window.location.replace(\"").append(HtmlUtils.htmlEscape(targetUrl)).append("\");\n");
        html.append("  </script>\n");
        html.append("  <noscript>\n");
        html.append("    <meta http-equiv=\"refresh\" content=\"0;url=").append(HtmlUtils.htmlEscape(targetUrl)).append("\">\n");
        html.append("  </noscript>\n");
        html.append("</head>\n<body>\n");
        html.append("  <p>Đang chuyển hướng đến bài viết: <a href=\"").append(HtmlUtils.htmlEscape(targetUrl)).append("\">")
                .append(HtmlUtils.htmlEscape(rawTitle)).append("</a>...</p>\n");
        html.append("</body>\n</html>");

        return html.toString();
    }
}
