package com.forum.service;

import com.forum.entity.User;
import com.forum.repository.UserRepository;
import com.forum.security.JwtUtils;
import com.forum.utils.Constants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.Random;

@Service
public class AuthService {

    @Value("${google.client.id:}")
    private String googleClientId;

    @Value("${google.client.secret:}")
    private String googleClientSecret;

    @Value("${google.token-info-url:https://oauth2.googleapis.com/tokeninfo}")
    private String googleTokenInfoUrl;

    @Value("${google.token-url:https://oauth2.googleapis.com/token}")
    private String googleTokenUrl;

    @Value("${google.user-info-url:https://www.googleapis.com/oauth2/v3/userinfo}")
    private String googleUserInfoUrl;

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

    public Map<String, Object> confirmEmailAndUpgradeRole(String token) {
        if (!org.springframework.util.StringUtils.hasText(token)) {
            throw new IllegalArgumentException("Mã xác thực email không hợp lệ");
        }

        Optional<User> userOpt = userRepository.findByEmailConfirmationToken(token);
        if (userOpt.isEmpty()) {
            throw new IllegalArgumentException("Mã xác nhận email không tồn tại hoặc tài khoản đã được kích hoạt trước đó.");
        }

        User user = userOpt.get();
        if (user.getEmailConfirmationExpiry() == null || user.getEmailConfirmationExpiry().isBefore(java.time.LocalDateTime.now())) {
            throw new IllegalArgumentException("EXPIRED:Liên kết xác minh email đã hết hạn (chỉ có hiệu lực trong 24h). Vui lòng bấm 'Gửi lại email xác nhận' để nhận liên kết mới.");
        }

        // Nâng cấp quyền lên ROLE_USER chính thức (Dùng HashSet khả biến để tránh UnsupportedOperationException)
        user.setRoles(new java.util.HashSet<>(Set.of(Constants.ROLE_USER)));
        user.setEmailConfirmationToken(null);
        user.setEmailConfirmationExpiry(null);
        userRepository.save(user);

        // Sinh JWT Token mới với quyền ROLE_USER
        String jwtToken = jwtUtils.generateJwtToken(user.getUsername(), user.getRoles());
        Map<String, Object> response = new java.util.HashMap<>();
        response.put("id", user.getId());
        response.put("token", jwtToken);
        response.put("username", user.getUsername());
        response.put("displayName", user.getDisplayName());
        response.put("roles", user.getRoles());
        response.put("avatar", user.getAvatar());
        response.put("email", user.getEmail());
        response.put("message", "Tài khoản của bạn đã được xác thực thành công!");
        return response;
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

    /**
     * Xác thực tính hợp lệ của Google ID Token qua endpoint chính thức của Google
     */
    private Map<String, Object> verifyGoogleToken(String idToken) {
        if (!org.springframework.util.StringUtils.hasText(idToken)) {
            throw new IllegalArgumentException("Google ID Token không được để trống.");
        }

        // Hỗ trợ mock token cho môi trường dev/kiểm thử nội bộ nếu cần
        if (idToken.startsWith("mock-google-token:")) {
            String mockEmail = idToken.replace("mock-google-token:", "").trim().toLowerCase();
            Map<String, Object> mock = new java.util.HashMap<>();
            mock.put("email", mockEmail);
            mock.put("name", mockEmail.split("@")[0]);
            mock.put("picture", null);
            mock.put("email_verified", "true");
            return mock;
        }

        try {
            RestTemplate restTemplate = new RestTemplate();
            String url = googleTokenInfoUrl + "?id_token=" + idToken;
            @SuppressWarnings("unchecked")
            Map<String, Object> body = restTemplate.getForObject(url, Map.class);
            if (body == null || !body.containsKey("email")) {
                throw new IllegalArgumentException("Token Google không hợp lệ hoặc đã hết hạn.");
            }

            String emailVerified = String.valueOf(body.get("email_verified"));
            if (!"true".equalsIgnoreCase(emailVerified)) {
                throw new IllegalArgumentException("Địa chỉ email Google chưa được xác thực.");
            }

            if (org.springframework.util.StringUtils.hasText(googleClientId)) {
                String aud = String.valueOf(body.get("aud"));
                if (!googleClientId.trim().equals(aud)) {
                    throw new IllegalArgumentException("Token Google không khớp với Client ID của diễn đàn.");
                }
            }

            return body;
        } catch (Exception e) {
            throw new IllegalArgumentException("Xác thực tài khoản Google thất bại: " + e.getMessage());
        }
    }

    /**
     * Đổi Authorization Code lấy Tokens từ Google (OAuth2 redirect flow),
     * sau đó xác thực và xử lý đăng nhập / hoàn tất thông tin đăng ký.
     */
    public Map<String, Object> exchangeGoogleCode(String code, String redirectUri) {
        if (!org.springframework.util.StringUtils.hasText(code)) {
            throw new IllegalArgumentException("Mã xác thực Google (code) không hợp lệ.");
        }

        // Hỗ trợ Mock code cho môi trường test/dev nếu cần
        if (code.startsWith("mock-google-code:")) {
            String mockEmail = code.substring("mock-google-code:".length()).trim();
            return processGoogleAuth("mock-google-token:" + mockEmail);
        }

        try {
            RestTemplate restTemplate = new RestTemplate();
            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.setContentType(org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED);

            org.springframework.util.MultiValueMap<String, String> map = new org.springframework.util.LinkedMultiValueMap<>();
            map.add("code", code);
            map.add("client_id", googleClientId != null ? googleClientId.trim() : "");
            map.add("client_secret", googleClientSecret != null ? googleClientSecret.trim() : "");
            map.add("redirect_uri", redirectUri != null ? redirectUri.trim() : "");
            map.add("grant_type", "authorization_code");

            org.springframework.http.HttpEntity<org.springframework.util.MultiValueMap<String, String>> requestEntity =
                    new org.springframework.http.HttpEntity<>(map, headers);

            @SuppressWarnings("unchecked")
            Map<String, Object> responseBody = restTemplate.postForObject(googleTokenUrl, requestEntity, Map.class);

            if (responseBody == null || !responseBody.containsKey("id_token")) {
                throw new IllegalArgumentException("Không thể nhận id_token từ Google.");
            }

            String idToken = (String) responseBody.get("id_token");
            return processGoogleAuth(idToken);
        } catch (org.springframework.web.client.HttpStatusCodeException e) {
            throw new IllegalArgumentException("Google từ chối đổi mã xác thực: " + e.getResponseBodyAsString());
        } catch (Exception e) {
            throw new IllegalArgumentException("Đổi mã xác thực Google thất bại: " + e.getMessage());
        }
    }

    /**
     * Bước kiểm tra tài khoản Google:
     * - Nếu email đã tồn tại -> Đăng nhập ngay, cấp JWT token (isExistingUser = true)
     * - Nếu email chưa tồn tại -> Trả về thông tin email, tên gợi ý để người dùng nhập Tên hiển thị (isExistingUser = false)
     */
    public Map<String, Object> processGoogleAuth(String idToken) {
        Map<String, Object> googlePayload = verifyGoogleToken(idToken);
        String email = ((String) googlePayload.get("email")).trim().toLowerCase();
        String name = (String) googlePayload.get("name");
        String picture = (String) googlePayload.get("picture");

        Optional<User> userOpt = userRepository.findFirstByEmail(email);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            boolean updated = false;

            // Nếu tài khoản cũ đang ở vai trò NON_OFFICIAL, vì đã đăng nhập bằng Google xác thực nên nâng cấp lên ROLE_USER
            if (user.getRoles() != null && user.getRoles().contains(Constants.ROLE_NON_OFFICIAL_USER)) {
                user.getRoles().remove(Constants.ROLE_NON_OFFICIAL_USER);
                user.getRoles().add(Constants.ROLE_USER);
                updated = true;
            }
            // Cập nhật avatar nếu chưa có
            if ((user.getAvatar() == null || !user.getAvatar().startsWith("http")) && picture != null && !picture.isEmpty()) {
                user.setAvatar(picture);
                updated = true;
            }
            if (updated) {
                userRepository.save(user);
            }

            String token = jwtUtils.generateJwtToken(user.getUsername(), user.getRoles());
            Map<String, Object> response = new java.util.HashMap<>();
            response.put("isExistingUser", true);
            response.put("id", user.getId());
            response.put("token", token);
            response.put("username", user.getUsername());
            response.put("displayName", user.getDisplayName());
            response.put("roles", user.getRoles());
            response.put("avatar", user.getAvatar());
            response.put("email", user.getEmail());
            response.put("message", "Đăng nhập Google thành công");
            return response;
        } else {
            // Tài khoản chưa tồn tại -> Cần nhập Tên hiển thị cá nhân hóa
            Map<String, Object> response = new java.util.HashMap<>();
            response.put("isExistingUser", false);
            response.put("email", email);
            response.put("suggestedDisplayName", (name != null && !name.trim().isEmpty()) ? name.trim() : email.split("@")[0]);
            response.put("avatar", picture);
            response.put("idToken", idToken);
            return response;
        }
    }

    /**
     * Hoàn tất đăng ký bằng Google sau khi người dùng nhập Tên hiển thị (Display Name):
     * Cấp thẳng ROLE_USER (Thành viên chính thức), bỏ qua bước xác thực email thủ công.
     */
    public Map<String, Object> completeGoogleRegistration(String idToken, String displayName) {
        if (!org.springframework.util.StringUtils.hasText(displayName)) {
            throw new IllegalArgumentException("Vui lòng nhập tên hiển thị.");
        }
        String cleanDisplayName = displayName.trim();
        if (cleanDisplayName.length() < 2 || cleanDisplayName.length() > 30) {
            throw new IllegalArgumentException("Tên hiển thị phải từ 2 đến 30 ký tự.");
        }

        Map<String, Object> googlePayload = verifyGoogleToken(idToken);
        String email = ((String) googlePayload.get("email")).trim().toLowerCase();
        String picture = (String) googlePayload.get("picture");

        // Kiểm tra tránh race condition nếu đã có tài khoản
        Optional<User> existingUser = userRepository.findFirstByEmail(email);
        if (existingUser.isPresent()) {
            User user = existingUser.get();
            String token = jwtUtils.generateJwtToken(user.getUsername(), user.getRoles());
            Map<String, Object> response = new java.util.HashMap<>();
            response.put("id", user.getId());
            response.put("token", token);
            response.put("username", user.getUsername());
            response.put("displayName", user.getDisplayName());
            response.put("roles", user.getRoles());
            response.put("avatar", user.getAvatar());
            response.put("email", user.getEmail());
            response.put("message", "Đăng nhập thành công.");
            return response;
        }

        // Tự động sinh username duy nhất từ email
        String finalUsername = generateUniqueUsernameFromEmail(email);

        User newUser = new User();
        newUser.setUsername(finalUsername);
        newUser.setEmail(email);
        newUser.setDisplayName(cleanDisplayName);
        newUser.setPassword(passwordEncoder.encode(java.util.UUID.randomUUID().toString()));
        newUser.setAvatar((picture != null && !picture.trim().isEmpty()) ? picture : getRandomColor());
        // Cấp ngay quyền Thành viên chính thức
        newUser.setRoles(new java.util.HashSet<>(Set.of(Constants.ROLE_USER)));

        userRepository.save(newUser);

        String token = jwtUtils.generateJwtToken(newUser.getUsername(), newUser.getRoles());
        Map<String, Object> response = new java.util.HashMap<>();
        response.put("id", newUser.getId());
        response.put("token", token);
        response.put("username", newUser.getUsername());
        response.put("displayName", newUser.getDisplayName());
        response.put("roles", newUser.getRoles());
        response.put("avatar", newUser.getAvatar());
        response.put("email", newUser.getEmail());
        response.put("message", "Đăng ký tài khoản Google thành công.");
        return response;
    }
}
