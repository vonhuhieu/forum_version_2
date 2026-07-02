package com.forum.controller;

import com.forum.dto.ResponseDTO;
import com.forum.dto.UserDTO;
import com.forum.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/search")
    public ResponseEntity<ResponseDTO<Page<UserDTO>>> searchUsers(
            @RequestParam(required = false, defaultValue = "") String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        try {
            String currentUsername = null;
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getPrincipal() instanceof String) {
                currentUsername = (String) auth.getPrincipal();
            }
            Page<UserDTO> result = userService.searchUsers(keyword, currentUsername, page, size);
            return ResponseEntity.ok(ResponseDTO.success(result));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ResponseDTO.fail(null));
        }
    }

    @GetMapping("/by-name")
    public ResponseEntity<ResponseDTO<UserDTO>> getUserByName(@RequestParam String name) {
        try {
            return userService.getUserByName(name)
                    .map(dto -> ResponseEntity.ok(ResponseDTO.success(dto)))
                    .orElseGet(() -> ResponseEntity.notFound().build());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ResponseDTO.fail(null));
        }
    }

    @PutMapping("/me/avatar")
    public ResponseEntity<ResponseDTO<UserDTO>> updateMyAvatar(@RequestBody Map<String, String> payload) {
        try {
            String currentUsername = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            String avatarUrl = payload.get("avatar");
            UserDTO result = userService.updateMyAvatar(currentUsername, avatarUrl);
            return ResponseEntity.ok(ResponseDTO.success(result));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ResponseDTO.fail(null));
        }
    }

    @PutMapping("/me/banner")
    public ResponseEntity<ResponseDTO<UserDTO>> updateMyBanner(@RequestBody Map<String, String> payload) {
        try {
            String currentUsername = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            String bannerUrl = payload.get("banner");
            UserDTO result = userService.updateMyBanner(currentUsername, bannerUrl);
            return ResponseEntity.ok(ResponseDTO.success(result));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ResponseDTO.fail(null));
        }
    }

    @PutMapping("/me/change-password")
    public ResponseEntity<ResponseDTO<Void>> changePassword(@RequestBody Map<String, String> payload) {
        try {
            String currentUsername = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            String currentPassword = payload.get("currentPassword");
            String newPassword = payload.get("newPassword");
            String confirmPassword = payload.get("confirmPassword");

            if (currentPassword == null || currentPassword.isEmpty() ||
                newPassword == null || newPassword.isEmpty() ||
                confirmPassword == null || confirmPassword.isEmpty()) {
                return ResponseEntity.badRequest().body(ResponseDTO.fail(null, "Tất cả các trường mật khẩu đều bắt buộc."));
            }

            if (!newPassword.equals(confirmPassword)) {
                return ResponseEntity.badRequest().body(ResponseDTO.fail(null, "Mật khẩu mới và xác nhận mật khẩu không khớp."));
            }

            userService.changePassword(currentUsername, currentPassword, newPassword);
            return ResponseEntity.ok(ResponseDTO.success(null));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ResponseDTO.fail(null, e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ResponseDTO.fail(null, "Có lỗi xảy ra khi đổi mật khẩu."));
        }
    }

    @PostMapping("/me/active")
    public ResponseEntity<ResponseDTO<Void>> updateActive() {
        try {
            String currentUsername = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            userService.updateLastActive(currentUsername);
            return ResponseEntity.ok(ResponseDTO.success(null));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ResponseDTO.fail(null));
        }
    }

    @GetMapping("/admin")
    public ResponseEntity<ResponseDTO<?>> getAdminUsers(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortOrder,
            @RequestParam(required = false) String role) {
        try {
            String currentUsername = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            if (page != null && size != null) {
                com.forum.dto.PageResponseDTO<UserDTO> result = userService.getAdminUsersPaged(
                        currentUsername, keyword, role, sortBy, sortOrder, page, size);
                return ResponseEntity.ok(ResponseDTO.success(result));
            } else {
                List<UserDTO> result = userService.getAdminUsers(currentUsername);
                return ResponseEntity.ok(ResponseDTO.success(result));
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ResponseDTO.fail(null));
        }
    }

    @PostMapping("/admin")
    public ResponseEntity<ResponseDTO<UserDTO>> adminCreateUser(@RequestBody Map<String, Object> payload) {
        try {
            String currentUsername = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            UserDTO result = userService.adminCreateUser(payload, currentUsername);
            return ResponseEntity.ok(ResponseDTO.success(result));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ResponseDTO.fail(null));
        }
    }

    @PutMapping("/admin/{id}")
    public ResponseEntity<ResponseDTO<UserDTO>> adminUpdateUser(@PathVariable Long id, @RequestBody Map<String, Object> payload) {
        try {
            String currentUsername = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            UserDTO result = userService.adminUpdateUser(id, payload, currentUsername);
            return ResponseEntity.ok(ResponseDTO.success(result));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ResponseDTO.fail(null));
        }
    }

    @DeleteMapping("/admin/{id}")
    public ResponseEntity<ResponseDTO<Void>> adminDeleteUser(@PathVariable Long id) {
        try {
            String currentUsername = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            userService.adminDeleteUser(id, currentUsername);
            return ResponseEntity.ok(ResponseDTO.success(null));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(ResponseDTO.fail(null));
        }
    }
}
