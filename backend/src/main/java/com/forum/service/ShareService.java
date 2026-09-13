package com.forum.service;

import com.forum.entity.Thread;
import com.forum.repository.ThreadRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

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
     * Thoát ký tự đặc biệt cho thuộc tính HTML mà vẫn giữ nguyên tiếng Việt Unicode
     */
    private String escapeMeta(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                   .replace("\"", "&quot;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;");
    }

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

        // Đảm bảo URL chuẩn HTTPS khi chạy sau Nginx reverse proxy
        String currentShareUrl = request.getRequestURL().toString();
        String forwardedProto = request.getHeader("X-Forwarded-Proto");
        if ("https".equalsIgnoreCase(forwardedProto) && currentShareUrl.startsWith("http://")) {
            currentShareUrl = "https://" + currentShareUrl.substring(7);
        }
        if (request.getQueryString() != null && !request.getQueryString().isEmpty()) {
            currentShareUrl += "?" + request.getQueryString();
        }

        Optional<Thread> threadOpt = threadRepository.findById(threadId);
        if (threadOpt.isEmpty()) {
            String fallbackUrl = baseFront != null ? baseFront + "/" : "/";
            log.warn("Social share threadId={} not found, fallback to home: {}", threadId, fallbackUrl);
            return "<!DOCTYPE html><html><head>"
                    + "<script>window.location.replace('" + escapeMeta(fallbackUrl) + "');</script>"
                    + "<noscript><meta http-equiv=\"refresh\" content=\"0;url=" + escapeMeta(fallbackUrl) + "\"></noscript>"
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

        // Tìm ảnh đầu tiên trong nội dung, nếu không có dùng ảnh logo mặc định của diễn đàn
        String imageUrl = "";
        Matcher matcher = IMG_PATTERN.matcher(rawContent);
        if (matcher.find()) {
            imageUrl = matcher.group(1);
        } else if (baseFront != null && !baseFront.isEmpty()) {
            imageUrl = baseFront + "/favicon-512x512.png";
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
        html.append("  <title>").append(escapeMeta(displayTitle)).append("</title>\n");
        html.append("  <meta name=\"description\" content=\"").append(escapeMeta(plainText)).append("\">\n");

        // Open Graph Meta Tags cho Facebook, Zalo, Twitter, LinkedIn...
        html.append("  <meta property=\"og:type\" content=\"article\">\n");
        html.append("  <meta property=\"og:site_name\" content=\"").append(escapeMeta(SITE_NAME)).append("\">\n");
        html.append("  <meta property=\"og:title\" content=\"").append(escapeMeta(rawTitle)).append("\">\n");
        html.append("  <meta property=\"og:description\" content=\"").append(escapeMeta(plainText)).append("\">\n");
        // og:url trỏ về URL web bài viết để Facebook hiển thị đúng tên miền hệ thống
        html.append("  <meta property=\"og:url\" content=\"").append(escapeMeta(targetUrl)).append("\">\n");

        if (!imageUrl.isEmpty()) {
            html.append("  <meta property=\"og:image\" content=\"").append(escapeMeta(imageUrl)).append("\">\n");
            html.append("  <meta name=\"twitter:image\" content=\"").append(escapeMeta(imageUrl)).append("\">\n");
        }

        html.append("  <meta name=\"twitter:card\" content=\"summary_large_image\">\n");
        html.append("  <meta name=\"twitter:title\" content=\"").append(escapeMeta(rawTitle)).append("\">\n");
        html.append("  <meta name=\"twitter:description\" content=\"").append(escapeMeta(plainText)).append("\">\n");

        // Chuyển hướng người dùng thật bằng JavaScript.
        // Bot (Facebook, Zalo, Twitter) không chạy JavaScript nên sẽ ở lại đọc trọn vẹn thẻ Open Graph.
        html.append("  <script type=\"text/javascript\">\n");
        html.append("    window.location.replace(\"").append(escapeMeta(targetUrl)).append("\");\n");
        html.append("  </script>\n");
        html.append("  <noscript>\n");
        html.append("    <meta http-equiv=\"refresh\" content=\"0;url=").append(escapeMeta(targetUrl)).append("\">\n");
        html.append("  </noscript>\n");
        html.append("</head>\n<body>\n");
        html.append("  <p>Đang chuyển hướng đến bài viết: <a href=\"").append(escapeMeta(targetUrl)).append("\">")
                .append(escapeMeta(rawTitle)).append("</a>...</p>\n");
        html.append("</body>\n</html>");

        return html.toString();
    }
}
