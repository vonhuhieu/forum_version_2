package com.forum.controller;

import com.forum.service.AuthService;
import com.forum.service.TurnstileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    @Autowired
    private TurnstileService turnstileService;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> loginRequest) {
        String username = loginRequest.get("username");
        String password = loginRequest.get("password");

        Map<String, Object> authData = authService.authenticateUser(username, password);
        if (authData != null) {
            return ResponseEntity.ok(authData);
        } else {
            return ResponseEntity.status(401).body(Map.of("message", "Sai tài khoản hoặc mật khẩu"));
        }
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Map<String, String> registerRequest) {
        String username = registerRequest.get("username");
        String password = registerRequest.get("password");
        String email = registerRequest.get("email");
        String displayName = registerRequest.get("displayName");
        
        String turnstileToken = registerRequest.get("turnstileToken");
        if (turnstileToken == null) {
            turnstileToken = registerRequest.get("recaptcha");
        }

        try {
            turnstileService.verifyTokenOrThrow(turnstileToken);
            authService.registerUser(username, password, email, displayName);
            return ResponseEntity.ok(Map.of("message", "Đăng ký thành công"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody Map<String, String> request) {
        String username = request.get("username");
        String email = request.get("email");
        try {
            authService.generatePasswordResetCode(username, email);
            return ResponseEntity.ok(Map.of("message", "Mã xác nhận đã được gửi đến email của bạn"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        String code = request.get("code");
        String newPassword = request.get("newPassword");
        try {
            authService.resetPasswordWithCode(email, code, newPassword);
            return ResponseEntity.ok(Map.of("message", "Đổi mật khẩu thành công"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
}
