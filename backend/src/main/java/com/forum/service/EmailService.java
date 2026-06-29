package com.forum.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Service
public class EmailService {

    @Value("${resend.api.key}")
    private String resendApiKey;

    @Value("${resend.from.email}")
    private String fromEmail;

    @Value("${resend.api.url:https://api.resend.com/emails}")
    private String resendApiUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * Gửi email bất đồng bộ qua Resend HTTP API (HTTPS port 443).
     * Không phụ thuộc SMTP port 587/465 — không bao giờ bị block bởi VPS.
     * Không block luồng HTTP request, đảm bảo API trả về nhanh.
     */
    @Async
    public void sendEmailAsync(String to, String subject, String content) {
        System.out.println("Gửi email tới: " + to + " | Tiêu đề: " + subject + " | Nội dung: " + content);
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(resendApiKey);

            Map<String, Object> body = Map.of(
                "from", fromEmail,
                "to", List.of(to),
                "subject", subject,
                "text", content
            );

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(
                resendApiUrl, request, String.class
            );

            System.out.println("Gửi email thành công. Status: " + response.getStatusCode());
        } catch (Exception e) {
            System.out.println("Lỗi gửi email qua Resend: " + e.getMessage());
        }
    }
}
