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

    public Map<String, Object> authenticateUser(String identifier, String password) {
        if (identifier == null || identifier.trim().isEmpty() || password == null) {
            return null;
        }
        String trimmed = identifier.trim();

        // 1. Ưu tiên tìm theo Email (không phân biệt hoa/thường)
        Optional<User> userOpt = userRepository.findFirstByEmail(trimmed.toLowerCase());

        // 2. Nếu không tìm thấy bằng email, thử tìm theo username (để tương thích ngược với tài khoản cũ như admin)
        if (userOpt.isEmpty()) {
            userOpt = userRepository.findByUsername(trimmed)
                    .filter(user -> user.getUsername().equals(trimmed));
        }

        userOpt = userOpt.filter(user -> passwordEncoder.matches(password, user.getPassword()));

        if (userOpt.isPresent()) {
            User user = userOpt.get();
            
            // Generate avatar for existing users if missing
            if (user.getAvatar() == null) {
                user.setAvatar(getRandomColor());
                userRepository.save(user);
            }

            String token = jwtUtils.generateJwtToken(user.getUsername(), user.getRoles());
            java.util.Map<String, Object> response = new java.util.HashMap<>();
            response.put("id", user.getId());
            response.put("token", token);
            response.put("username", user.getUsername());
            response.put("displayName", user.getDisplayName()); // Có thể null
            response.put("roles", user.getRoles());
            response.put("avatar", user.getAvatar());
            response.put("email", user.getEmail());
            return response;
        }
        return null;
    }

    private String getRandomColor() {
        int hue = new Random().nextInt(360);
        // Trả về màu HSL sáng đẹp (Hue, 70% bão hòa, 45% độ sáng)
        return String.format("hsl(%d, 70%%, 45%%)", hue);
    }

    private String generateUniqueUsernameFromEmail(String email) {
        if (email == null || !email.contains("@")) {
            return "user_" + java.util.UUID.randomUUID().toString().substring(0, 8);
        }
        String prefix = email.split("@")[0].toLowerCase();
        String cleanPrefix = prefix.replaceAll("[^a-z0-9_]", "_");
        cleanPrefix = cleanPrefix.replaceAll("^_+|_+$", "");
        if (cleanPrefix.length() < 3) {
            cleanPrefix = "user_" + cleanPrefix;
        }
        if (cleanPrefix.length() > 15) {
            cleanPrefix = cleanPrefix.substring(0, 15);
        }

        String candidate = cleanPrefix;
        int counter = 1;
        while (userRepository.findByUsername(candidate).isPresent()) {
            int randomSuffix = new Random().nextInt(900) + 100;
            candidate = cleanPrefix + "_" + randomSuffix;
            counter++;
            if (counter > 20) {
                candidate = "u_" + java.util.UUID.randomUUID().toString().substring(0, 8);
                break;
            }
        }
        return candidate;
    }

    public Map<String, Object> registerUser(String username, String password, String email, String displayName, String baseUrl) {
        if (!org.springframework.util.StringUtils.hasText(email) || !email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            throw new IllegalArgumentException("Địa chỉ email không hợp lệ");
        }
        String normalizedEmail = email.trim().toLowerCase();

        if (userRepository.findFirstByEmail(normalizedEmail).isPresent()) {
            throw new IllegalArgumentException("Email đã được sử dụng");
        }

        // Tự động sinh username nếu không được truyền từ phía client
        String finalUsername;
        if (org.springframework.util.StringUtils.hasText(username)) {
            finalUsername = username.trim();
            if (!finalUsername.matches("^[a-zA-Z0-9_]{3,20}$")) {
                throw new IllegalArgumentException("Tên đăng nhập không hợp lệ. Chỉ bao gồm chữ cái, số, gạch dưới (3-20 ký tự) và KHÔNG dấu/khoảng trắng.");
            }
            if (userRepository.findByUsername(finalUsername).isPresent()) {
                throw new IllegalArgumentException("Tên đăng nhập đã tồn tại");
            }
        } else {
            finalUsername = generateUniqueUsernameFromEmail(normalizedEmail);
        }

        if (!org.springframework.util.StringUtils.hasText(password) || password.length() < 3) {
            throw new IllegalArgumentException("Mật khẩu phải có ít nhất 3 ký tự");
        }

        User user = new User();
        user.setUsername(finalUsername);
        user.setPassword(passwordEncoder.encode(password));
        user.setEmail(normalizedEmail);
        
        if (org.springframework.util.StringUtils.hasText(displayName)) {
            user.setDisplayName(displayName.trim());
        } else {
            String emailPrefix = normalizedEmail.split("@")[0];
            user.setDisplayName(emailPrefix);
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
            emailService.sendConfirmationEmailSync(normalizedEmail, user.getDisplayName(), token, baseUrl);
            emailSent = true;
        } catch (Exception e) {
            System.err.println("CẢNH BÁO: Thử gửi email xác thực khi đăng ký thất bại: " + e.getMessage());
            emailSent = false;
        }

        // Sinh JWT Token tự động đăng nhập vai trò ROLE_NON_OFFICIAL_USER
        String jwtToken = jwtUtils.generateJwtToken(finalUsername, user.getRoles());
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

    public void confirmEmailAndUpgradeRole(String token, String password, String newPassword) {
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

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new IllegalArgumentException("Mật khẩu không chính xác.");
        }

        // Nếu có truyền mật khẩu mới thì cập nhật, nếu không thì giữ nguyên mật khẩu ban đầu
        if (org.springframework.util.StringUtils.hasText(newPassword)) {
            if (newPassword.trim().length() < 3) {
                throw new IllegalArgumentException("Mật khẩu mới phải có ít nhất 3 ký tự.");
            }
            if (passwordEncoder.matches(newPassword, user.getPassword())) {
                throw new IllegalArgumentException("Mật khẩu mới phải khác với mật khẩu hiện tại.");
            }
            user.setPassword(passwordEncoder.encode(newPassword));
        }

        // Nâng cấp quyền lên ROLE_USER chính thức (Dùng HashSet khả biến để tránh UnsupportedOperationException)
        user.setRoles(new java.util.HashSet<>(Set.of(Constants.ROLE_USER)));
        user.setEmailConfirmationToken(null);
        user.setEmailConfirmationExpiry(null);
        userRepository.save(user);
    }

    public void generatePasswordResetCode(String email) {
        if (!org.springframework.util.StringUtils.hasText(email)) {
            throw new IllegalArgumentException("Vui lòng cung cấp địa chỉ email");
        }
        String normalizedEmail = email.trim().toLowerCase();
        Optional<User> userOpt = userRepository.findFirstByEmail(normalizedEmail);
        if (userOpt.isEmpty()) {
            throw new IllegalArgumentException("Email không tồn tại trong hệ thống");
        }

        User user = userOpt.get();
        String code = String.format("%06d", new Random().nextInt(999999));
        user.setResetCode(code);
        user.setResetCodeExpiry(java.time.LocalDateTime.now().plusMinutes(15));
        userRepository.save(user);

        System.out.println("Mã reset mật khẩu cho email " + normalizedEmail + " là: " + code);

        emailService.sendEmailAsync(
            user.getEmail(),
            "Mã xác nhận lấy lại mật khẩu - Diễn đàn",
            "Mã xác nhận của bạn là: " + code + "\nMã này sẽ hết hạn sau 15 phút."
        );
    }

    public void generatePasswordResetCode(String username, String email) {
        generatePasswordResetCode(email);
    }

    public void resetPasswordWithCode(String email, String code, String newPassword) {
        if (!org.springframework.util.StringUtils.hasText(email)) {
            throw new IllegalArgumentException("Vui lòng cung cấp địa chỉ email");
        }
        Optional<User> userOpt = userRepository.findFirstByEmail(email.trim().toLowerCase());
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
