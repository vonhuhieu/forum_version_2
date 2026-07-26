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
        if (resendApiKey == null || resendApiKey.trim().isEmpty()) {
            System.err.println("LỖI GỬI EMAIL: Biến môi trường RESEND_API_KEY đang bị TRỐNG hoặc ứng dụng chưa được RESTART trong IntelliJ!");
            return;
        }
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(resendApiKey.trim());

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

    @Value("${app.frontend.url:http://localhost:5173}")
    private String frontendUrl;

    /**
     * Gửi email xác thực tài khoản kích hoạt tự động với domain động từ frontend
     */
    @Async
    public void sendConfirmationEmailAsync(String to, String username, String token, String baseUrl) {
        String base = (baseUrl != null && !baseUrl.trim().isEmpty()) ? baseUrl.trim() : frontendUrl;
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        String confirmUrl = base + "/confirm-email?token=" + token;
        String subject = "HTXSL Forum - Yêu cầu xác nhận email tài khoản";
        
        String textContent = "Xin chào " + username + ",\n\n"
            + "Để hoàn tất đăng ký tài khoản tại HTXSL Forum, vui lòng xác nhận địa chỉ email của bạn bằng cách truy cập liên kết sau:\n"
            + confirmUrl + "\n\n"
            + "Liên kết này có hiệu lực trong 24 giờ.";

        String htmlContent = "<div style=\"font-family: Arial, sans-serif; background-color: #f4f6f9; padding: 30px; color: #333;\">"
            + "<div style=\"max-width: 600px; margin: 0 auto; background: #ffffff; border-radius: 8px; overflow: hidden; box-shadow: 0 2px 8px rgba(0,0,0,0.08);\">"
            + "<div style=\"background-color: #1a507a; padding: 20px; text-align: center; color: #ffffff;\">"
            + "<h2 style=\"margin: 0; font-size: 22px;\">HTXSL FORUM</h2>"
            + "</div>"
            + "<div style=\"padding: 30px; line-height: 1.6;\">"
            + "<p>Xin chào <strong>" + username + "</strong>,</p>"
            + "<p>Cảm ơn bạn đã đăng ký tài khoản. Để hoàn tất đăng ký và kích hoạt đầy đủ quyền thành viên, bạn cần xác nhận địa chỉ email bằng cách nhấn vào nút bên dưới:</p>"
            + "<div style=\"text-align: center; margin: 30px 0;\">"
            + "<a href=\"" + confirmUrl + "\" style=\"background-color: #1a507a; color: #ffffff; padding: 12px 28px; text-decoration: none; border-radius: 4px; font-weight: bold; display: inline-block;\">Xác nhận email</a>"
            + "</div>"
            + "<p style=\"font-size: 0.9em; color: #666;\">Hoặc copy liên kết sau dán vào trình duyệt của bạn:<br>"
            + "<a href=\"" + confirmUrl + "\" style=\"color: #1a507a; word-break: break-all;\">" + confirmUrl + "</a></p>"
            + "<p style=\"font-size: 0.85em; color: #888; margin-top: 25px;\"><i>Lưu ý: Liên kết xác nhận này có hiệu lực trong 24 giờ.</i></p>"
            + "</div>"
            + "<div style=\"background-color: #f8f9fa; padding: 15px; text-align: center; font-size: 0.8em; color: #777; border-top: 1px solid #eee;\">"
            + "© HTXSL Forum - Hệ thống tự động gửi email"
            + "</div>"
            + "</div>"
            + "</div>";

        System.out.println("Gửi email xác thực tới: " + to + " | Link: " + confirmUrl);
        if (resendApiKey == null || resendApiKey.trim().isEmpty()) {
            System.err.println("LỖI GỬI EMAIL: Biến môi trường RESEND_API_KEY đang bị TRỐNG hoặc ứng dụng chưa được RESTART trong IntelliJ!");
            return;
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(resendApiKey.trim());

            Map<String, Object> body = Map.of(
                "from", fromEmail,
                "to", List.of(to),
                "subject", subject,
                "text", textContent,
                "html", htmlContent
            );

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(resendApiUrl, request, String.class);
            System.out.println("Gửi email xác thực thành công. Status: " + response.getStatusCode());
        } catch (Exception e) {
            System.out.println("Lỗi gửi email xác thực qua Resend: " + e.getMessage());
        }
    }

    /**
     * Gửi email xác thực tài khoản đồng bộ (Throw ngoại lệ nếu thất bại để API báo lỗi 400 thực tế)
     */
    public void sendConfirmationEmailSync(String to, String username, String token, String baseUrl) {
        String base = (baseUrl != null && !baseUrl.trim().isEmpty()) ? baseUrl.trim() : frontendUrl;
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        String confirmUrl = base + "/confirm-email?token=" + token;
        String subject = "HTXSL Forum - Yêu cầu xác nhận email tài khoản";
        
        String textContent = "Xin chào " + username + ",\n\n"
            + "Để hoàn tất đăng ký tài khoản tại HTXSL Forum, vui lòng xác nhận địa chỉ email của bạn bằng cách truy cập liên kết sau:\n"
            + confirmUrl + "\n\n"
            + "Liên kết này có hiệu lực trong 24 giờ.";

        String htmlContent = "<div style=\"font-family: Arial, sans-serif; background-color: #f4f6f9; padding: 30px; color: #333;\">"
            + "<div style=\"max-width: 600px; margin: 0 auto; background: #ffffff; border-radius: 8px; overflow: hidden; box-shadow: 0 2px 8px rgba(0,0,0,0.08);\">"
            + "<div style=\"background-color: #1a507a; padding: 20px; text-align: center; color: #ffffff;\">"
            + "<h2 style=\"margin: 0; font-size: 22px;\">HTXSL FORUM</h2>"
            + "</div>"
            + "<div style=\"padding: 30px; line-height: 1.6;\">"
            + "<p>Xin chào <strong>" + username + "</strong>,</p>"
            + "<p>Cảm ơn bạn đã đăng ký tài khoản. Để hoàn tất đăng ký và kích hoạt đầy đủ quyền thành viên, bạn cần xác nhận địa chỉ email bằng cách nhấn vào nút bên dưới:</p>"
            + "<div style=\"text-align: center; margin: 30px 0;\">"
            + "<a href=\"" + confirmUrl + "\" style=\"background-color: #1a507a; color: #ffffff; padding: 12px 28px; text-decoration: none; border-radius: 4px; font-weight: bold; display: inline-block;\">Xác nhận email</a>"
            + "</div>"
            + "<p style=\"font-size: 0.9em; color: #666;\">Hoặc copy liên kết sau dán vào trình duyệt của bạn:<br>"
            + "<a href=\"" + confirmUrl + "\" style=\"color: #1a507a; word-break: break-all;\">" + confirmUrl + "</a></p>"
            + "<p style=\"font-size: 0.85em; color: #888; margin-top: 25px;\"><i>Lưu ý: Liên kết xác nhận này có hiệu lực trong 24 giờ.</i></p>"
            + "</div>"
            + "<div style=\"background-color: #f8f9fa; padding: 15px; text-align: center; font-size: 0.8em; color: #777; border-top: 1px solid #eee;\">"
            + "© HTXSL Forum - Hệ thống tự động gửi email"
            + "</div>"
            + "</div>"
            + "</div>";

        System.out.println("Gửi email xác thực (Sync) tới: " + to + " | Link: " + confirmUrl);
        if (resendApiKey == null || resendApiKey.trim().isEmpty()) {
            throw new IllegalArgumentException("Chưa cấu hình RESEND_API_KEY hoặc ứng dụng chưa được khởi động lại trong IntelliJ.");
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(resendApiKey.trim());

            Map<String, Object> body = Map.of(
                "from", fromEmail,
                "to", List.of(to),
                "subject", subject,
                "text", textContent,
                "html", htmlContent
            );

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(resendApiUrl, request, String.class);
            System.out.println("Gửi email xác thực (Sync) thành công. Status: " + response.getStatusCode());
        } catch (Exception e) {
            System.err.println("Lỗi gửi email xác thực qua Resend (Sync): " + e.getMessage());
            throw new IllegalArgumentException("Gửi email qua Resend thất bại: " + e.getMessage());
        }
    }
}
