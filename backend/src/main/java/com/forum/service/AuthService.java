package com.forum.service;

import com.forum.entity.User;
import com.forum.repository.UserRepository;
import com.forum.security.JwtUtils;
import com.forum.utils.Constants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.Random;

@Service
public class AuthService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private EmailService emailService;

    public Map<String, Object> authenticateUser(String username, String password) {
        Optional<User> userOpt = userRepository.findByUsername(username)
                .filter(user -> user.getUsername().equals(username)) // Bắt buộc khớp chính xác chữ hoa/thường
                .filter(user -> passwordEncoder.matches(password, user.getPassword()));

        if (userOpt.isPresent()) {
            User user = userOpt.get();
            
            // Generate avatar for existing users if missing
            if (user.getAvatar() == null) {
                user.setAvatar(getRandomColor());
                userRepository.save(user);
            }

            String token = jwtUtils.generateJwtToken(username, user.getRoles());
            java.util.Map<String, Object> response = new java.util.HashMap<>();
            response.put("id", user.getId());
            response.put("token", token);
            response.put("username", user.getUsername());
            response.put("displayName", user.getDisplayName()); // Có thể null
            response.put("roles", user.getRoles());
            response.put("avatar", user.getAvatar());
            return response;
        }
        return null;
    }

    private String getRandomColor() {
        int hue = new Random().nextInt(360);
        // Trả về màu HSL sáng đẹp (Hue, 70% bão hòa, 45% độ sáng)
        return String.format("hsl(%d, 70%%, 45%%)", hue);
    }

    public Map<String, Object> registerUser(String username, String password, String email, String displayName, String baseUrl) {
        // Kiểm định định dạng tài khoản nghiêm ngặt
        if (username == null || !username.matches("^[a-zA-Z0-9_]{3,20}$")) {
            throw new IllegalArgumentException("Tên đăng nhập không hợp lệ. Chỉ bao gồm chữ cái, số, gạch dưới (3-20 ký tự) và KHÔNG dấu/khoảng trắng.");
        }

        if (userRepository.findByUsername(username).isPresent()) {
            throw new IllegalArgumentException("Tên đăng nhập đã tồn tại");
        }
        if (userRepository.findFirstByEmail(email).isPresent()) {
            throw new IllegalArgumentException("Email đã được sử dụng");
        }

        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setEmail(email);
        
        if (org.springframework.util.StringUtils.hasText(displayName)) {
            user.setDisplayName(displayName.trim());
        } else {
            user.setDisplayName(username);
        }

        // Dùng HashSet khả biến để tránh UnsupportedOperationException trong Hibernate
        user.setRoles(new java.util.HashSet<>(Set.of(Constants.ROLE_NON_OFFICIAL_USER)));
        user.setAvatar(getRandomColor());

        // Sinh token xác nhận email (hạn 24 giờ)
        String token = java.util.UUID.randomUUID().toString();
        user.setEmailConfirmationToken(token);
        user.setEmailConfirmationExpiry(java.time.LocalDateTime.now().plusHours(24));

        userRepository.save(user);

        // Thử gửi email xác thực ban đầu (bắt ngoại lệ an toàn để đăng ký không bị đổ vỡ nếu rớt mạng)
        boolean emailSent = false;
        try {
            emailService.sendConfirmationEmailSync(email, user.getDisplayName(), token, baseUrl);
            emailSent = true;
        } catch (Exception e) {
            System.err.println("CẢNH BÁO: Thử gửi email xác thực khi đăng ký thất bại: " + e.getMessage());
            emailSent = false;
        }

        // Sinh JWT Token tự động đăng nhập vai trò ROLE_NON_OFFICIAL_USER
        String jwtToken = jwtUtils.generateJwtToken(username, user.getRoles());
        Map<String, Object> response = new java.util.HashMap<>();
        response.put("id", user.getId());
        response.put("token", jwtToken);
        response.put("username", user.getUsername());
        response.put("displayName", user.getDisplayName());
        response.put("roles", user.getRoles());
        response.put("avatar", user.getAvatar());
        response.put("email", user.getEmail());
        response.put("emailSent", emailSent);
        response.put("message", "Đăng ký thành công");
        return response;
    }

    public void resendConfirmationEmail(String email, String baseUrl) {
        if (!org.springframework.util.StringUtils.hasText(email)) {
            throw new IllegalArgumentException("Vui lòng cung cấp địa chỉ email");
        }

        Optional<User> userOpt = userRepository.findFirstByEmail(email.trim());
        if (userOpt.isEmpty()) {
            throw new IllegalArgumentException("Email không tồn tại trong hệ thống");
        }

        User user = userOpt.get();
        if (user.getRoles().contains(Constants.ROLE_USER) && !user.getRoles().contains(Constants.ROLE_NON_OFFICIAL_USER)) {
            throw new IllegalArgumentException("Tài khoản của bạn đã được xác minh trước đó. Vui lòng đăng nhập.");
        }

        String token = java.util.UUID.randomUUID().toString();
        user.setEmailConfirmationToken(token);
        user.setEmailConfirmationExpiry(java.time.LocalDateTime.now().plusHours(24));
        userRepository.save(user);

        // Gọi đồng bộ để ném ngoại lệ thực tế nếu Resend bị lỗi
        emailService.sendConfirmationEmailSync(user.getEmail(), user.getDisplayName() != null ? user.getDisplayName() : user.getUsername(), token, baseUrl);
    }

    public Map<String, Object> verifyConfirmationToken(String token) {
        if (!org.springframework.util.StringUtils.hasText(token)) {
            throw new IllegalArgumentException("Mã xác thực email không hợp lệ");
        }

        Optional<User> userOpt = userRepository.findByEmailConfirmationToken(token);
        if (userOpt.isEmpty()) {
            throw new IllegalArgumentException("Mã xác nhận email không tồn tại hoặc không hợp lệ.");
        }

        User user = userOpt.get();
        if (user.getEmailConfirmationExpiry() == null || user.getEmailConfirmationExpiry().isBefore(java.time.LocalDateTime.now())) {
            throw new IllegalArgumentException("EXPIRED:Liên kết xác minh email đã hết hạn (chỉ có hiệu lực trong 24h). Vui lòng bấm 'Gửi lại email xác nhận' để nhận liên kết mới.");
        }

        Map<String, Object> res = new java.util.HashMap<>();
        res.put("username", user.getUsername());
        res.put("displayName", user.getDisplayName());
        res.put("email", user.getEmail());
        return res;
    }

    public void confirmEmailAndUpgradeRole(String token, String currentPassword, String newPassword) {
        if (!org.springframework.util.StringUtils.hasText(token)) {
            throw new IllegalArgumentException("Mã xác thực email không hợp lệ");
        }

        Optional<User> userOpt = userRepository.findByEmailConfirmationToken(token);
        if (userOpt.isEmpty()) {
            throw new IllegalArgumentException("Mã xác nhận email không tồn tại hoặc không hợp lệ.");
        }

        User user = userOpt.get();
        if (user.getEmailConfirmationExpiry() == null || user.getEmailConfirmationExpiry().isBefore(java.time.LocalDateTime.now())) {
            throw new IllegalArgumentException("EXPIRED:Liên kết xác minh email đã hết hạn (chỉ có hiệu lực trong 24h). Vui lòng bấm 'Gửi lại email xác nhận' để nhận liên kết mới.");
        }

        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new IllegalArgumentException("Mật khẩu hiện tại không chính xác.");
        }

        if (!org.springframework.util.StringUtils.hasText(newPassword) || newPassword.trim().length() < 3) {
            throw new IllegalArgumentException("Mật khẩu mới phải có ít nhất 3 ký tự.");
        }

        if (passwordEncoder.matches(newPassword, user.getPassword())) {
            throw new IllegalArgumentException("Mật khẩu mới phải khác với mật khẩu hiện tại.");
        }

        // Đổi sang mật khẩu mới
        user.setPassword(passwordEncoder.encode(newPassword));

        // Nâng cấp quyền lên ROLE_USER chính thức (Dùng HashSet khả biến để tránh UnsupportedOperationException)
        user.setRoles(new java.util.HashSet<>(Set.of(Constants.ROLE_USER)));
        user.setEmailConfirmationToken(null);
        user.setEmailConfirmationExpiry(null);
        userRepository.save(user);
    }

    public void generatePasswordResetCode(String username, String email) {
        Optional<User> userOpt = userRepository.findFirstByEmail(email);
        // Kiểm tra cả email tồn tại lẫn username khớp — thông báo chung để tránh lộ thông tin
        if (userOpt.isEmpty() || !userOpt.get().getUsername().equals(username)) {
            throw new IllegalArgumentException("Tên đăng nhập hoặc email không chính xác");
        }

        User user = userOpt.get();
        String code = String.format("%06d", new Random().nextInt(999999));
        user.setResetCode(code);
        user.setResetCodeExpiry(java.time.LocalDateTime.now().plusMinutes(15));
        userRepository.save(user);

        System.out.println("Mã reset mật khẩu cho email " + email + " là: " + code);

        emailService.sendEmailAsync(
            email,
            "Mã xác nhận lấy lại mật khẩu - Diễn đàn",
            "Mã xác nhận của bạn là: " + code + "\nMã này sẽ hết hạn sau 15 phút."
        );
    }

    public void resetPasswordWithCode(String email, String code, String newPassword) {
        Optional<User> userOpt = userRepository.findFirstByEmail(email);
        if (userOpt.isEmpty()) {
            throw new IllegalArgumentException("Email không tồn tại trong hệ thống");
        }

        User user = userOpt.get();
        if (user.getResetCode() == null || !user.getResetCode().equals(code)) {
            throw new IllegalArgumentException("Mã xác nhận không chính xác");
        }

        if (user.getResetCodeExpiry() == null || user.getResetCodeExpiry().isBefore(java.time.LocalDateTime.now())) {
            throw new IllegalArgumentException("Mã xác nhận đã hết hạn");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setResetCode(null);
        user.setResetCodeExpiry(null);
        userRepository.save(user);
    }
}
