package com.forum.service;

import com.forum.entity.Category;
import com.forum.entity.Thread;
import com.forum.repository.CategoryRepository;
import com.forum.repository.ThreadRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SitemapService {

    private final ThreadRepository threadRepository;
    private final CategoryRepository categoryRepository;

    @Value("${app.frontend.url}")
    private String frontendUrl;

    /**
     * Xác định Base URL của Frontend:
     * 1. Ưu tiên biến cấu hình app.frontend.url (được đồng bộ tự động từ biến môi trường APP_FRONTEND_URL).
     * 2. Fallback động dựa trên HttpServletRequest (Host / X-Forwarded-Host) để đảm bảo linh hoạt tuyệt đối khi di trú domain.
     */
    private String resolveBaseUrl(HttpServletRequest request) {
        String base = frontendUrl;
        if (base != null && !base.trim().isEmpty()) {
            base = base.trim();
            if (base.endsWith("/")) {
                base = base.substring(0, base.length() - 1);
            }
            return base;
        }

        if (request != null) {
            String proto = request.getHeader("X-Forwarded-Proto");
            if (proto == null || proto.isEmpty()) {
                proto = request.isSecure() ? "https" : "http";
            }

            String host = request.getHeader("X-Forwarded-Host");
            if (host == null || host.isEmpty()) {
                host = request.getHeader("Host");
            }
            if (host == null || host.isEmpty()) {
                host = request.getServerName();
            }

            // Nếu host là api.domain.com -> tự động chuyển đổi thành domain.com của frontend
            if (host != null && host.startsWith("api.")) {
                host = host.substring(4);
            }

            return proto + "://" + host;
        }

        return "";
    }

    @Transactional(readOnly = true)
    public String generateSitemapXml(HttpServletRequest request) {
        String baseUrl = resolveBaseUrl(request);

        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">\n");

        // 1. Trang chủ
        xml.append("  <url>\n");
        xml.append("    <loc>").append(baseUrl).append("/</loc>\n");
        xml.append("    <changefreq>daily</changefreq>\n");
        xml.append("    <priority>1.0</priority>\n");
        xml.append("  </url>\n");

        // 2. Trang chủ đề mới
        xml.append("  <url>\n");
        xml.append("    <loc>").append(baseUrl).append("/latest</loc>\n");
        xml.append("    <changefreq>hourly</changefreq>\n");
        xml.append("    <priority>0.9</priority>\n");
        xml.append("  </url>\n");

        // 3. Trang thành viên
        xml.append("  <url>\n");
        xml.append("    <loc>").append(baseUrl).append("/thanh-vien</loc>\n");
        xml.append("    <changefreq>daily</changefreq>\n");
        xml.append("    <priority>0.8</priority>\n");
        xml.append("  </url>\n");

        // 4. Chuyên mục (Categories)
        try {
            List<Category> categories = categoryRepository.findAll();
            for (Category cat : categories) {
                xml.append("  <url>\n");
                xml.append("    <loc>").append(baseUrl).append("/category/").append(cat.getId()).append("</loc>\n");
                xml.append("    <changefreq>daily</changefreq>\n");
                xml.append("    <priority>0.8</priority>\n");
                xml.append("  </url>\n");
            }
        } catch (Exception e) {
            log.error("Lỗi khi nạp danh mục cho Sitemap: {}", e.getMessage());
        }

        // 5. Bài viết công khai (Threads)
        try {
            List<Thread> threads = threadRepository.findAllPublicOrderByLastPostAtDesc();
            for (Thread thread : threads) {
                if (thread.isActive()) {
                    xml.append("  <url>\n");
                    xml.append("    <loc>").append(baseUrl).append("/thread/").append(thread.getId()).append("</loc>\n");
                    if (thread.getLastPostAt() != null) {
                        xml.append("    <lastmod>").append(thread.getLastPostAt().format(DateTimeFormatter.ISO_LOCAL_DATE)).append("</lastmod>\n");
                    } else if (thread.getCreatedAt() != null) {
                        xml.append("    <lastmod>").append(thread.getCreatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE)).append("</lastmod>\n");
                    }
                    xml.append("    <changefreq>weekly</changefreq>\n");
                    xml.append("    <priority>0.7</priority>\n");
                    xml.append("  </url>\n");
                }
            }
        } catch (Exception e) {
            log.error("Lỗi khi nạp bài viết cho Sitemap: {}", e.getMessage());
        }

        xml.append("</urlset>");
        return xml.toString();
    }
}
