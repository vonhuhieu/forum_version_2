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
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.SimpleMailMessage;

@Service
public class AuthService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtils jwtUtils;

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

    public void registerUser(String username, String password, String email, String displayName) {
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

        user.setRoles(Set.of(Constants.ROLE_NON_OFFICIAL_USER));
        user.setAvatar(getRandomColor());
        userRepository.save(user);
    }

    @Autowired(required = false)
    private JavaMailSender mailSender;

    public void generatePasswordResetCode(String email) {
        Optional<User> userOpt = userRepository.findFirstByEmail(email);
        if (userOpt.isEmpty()) {
            throw new IllegalArgumentException("Email không tồn tại trong hệ thống");
        }

        User user = userOpt.get();
        String code = String.format("%06d", new Random().nextInt(999999));
        user.setResetCode(code);
        user.setResetCodeExpiry(java.time.LocalDateTime.now().plusMinutes(15));
        userRepository.save(user);

        System.out.println("Mã reset mật khẩu cho email " + email + " là: " + code);

        if (mailSender != null) {
            try {
                SimpleMailMessage message = new SimpleMailMessage();
                message.setTo(email);
                message.setSubject("Mã xác nhận lấy lại mật khẩu - Diễn đàn");
                message.setText("Mã xác nhận của bạn là: " + code + "\nMã này sẽ hết hạn sau 15 phút.");
                mailSender.send(message);
            } catch (Exception e) {
                System.out.println("Lỗi gửi email: " + e.getMessage());
                // Khong throw exception de van log code ra console cho muc dich test
            }
        }
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
