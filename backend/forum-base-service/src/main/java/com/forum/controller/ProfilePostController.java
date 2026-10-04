package com.forum.controller;

import com.forum.dto.*;
import com.forum.service.ProfilePostService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/profile-posts")
@RequiredArgsConstructor
public class ProfilePostController {

    private final ProfilePostService profilePostService;

    private String getCurrentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return null;
        }
        return (String) auth.getPrincipal();
    }

    @GetMapping("/user/{username}")
    public ResponseEntity<ResponseDTO<PageResponseDTO<ProfilePostDTO>>> getProfilePosts(
            @PathVariable String username,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        String currentUsername = getCurrentUsername();
        return ResponseEntity.ok(ResponseDTO.success(profilePostService.getProfilePostsByUsername(username, page, size, currentUsername)));
    }

    @GetMapping("/{id}/comments")
    public ResponseEntity<ResponseDTO<PageResponseDTO<ProfilePostCommentDTO>>> getComments(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "3") int size) {
        String currentUsername = getCurrentUsername();
        return ResponseEntity.ok(ResponseDTO.success(profilePostService.getCommentsByProfilePostId(id, page, size, currentUsername)));
    }

    @PostMapping
    public ResponseEntity<ResponseDTO<ProfilePostDTO>> createProfilePost(@RequestBody ProfilePostCreateDTO dto) {
        String currentUsername = getCurrentUsername();
        if (currentUsername == null) {
            return ResponseEntity.status(401).body(ResponseDTO.fail(null, "Vui lòng đăng nhập để đăng bài"));
        }
        return ResponseEntity.ok(ResponseDTO.success(profilePostService.createProfilePost(dto, currentUsername)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ResponseDTO<ProfilePostDTO>> updateProfilePost(
            @PathVariable Long id,
            @RequestBody Map<String, String> payload) {
        String currentUsername = getCurrentUsername();
        if (currentUsername == null) {
            return ResponseEntity.status(401).body(ResponseDTO.fail(null, "Vui lòng đăng nhập"));
        }
        String content = payload.get("content");
        return ResponseEntity.ok(ResponseDTO.success(profilePostService.updateProfilePost(id, content, currentUsername)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ResponseDTO<Void>> deleteProfilePost(@PathVariable Long id) {
        String currentUsername = getCurrentUsername();
        if (currentUsername == null) {
            return ResponseEntity.status(401).body(ResponseDTO.fail(null, "Vui lòng đăng nhập"));
        }
        profilePostService.deleteProfilePost(id, currentUsername);
        return ResponseEntity.ok(ResponseDTO.success(null));
    }

    @PostMapping("/{id}/comments")
    public ResponseEntity<ResponseDTO<ProfilePostCommentDTO>> createComment(
            @PathVariable Long id,
            @RequestBody ProfilePostCommentCreateDTO dto) {
        String currentUsername = getCurrentUsername();
        if (currentUsername == null) {
            return ResponseEntity.status(401).body(ResponseDTO.fail(null, "Vui lòng đăng nhập"));
        }
        return ResponseEntity.ok(ResponseDTO.success(profilePostService.createComment(id, dto, currentUsername)));
    }

    @PutMapping("/comments/{commentId}")
    public ResponseEntity<ResponseDTO<ProfilePostCommentDTO>> updateComment(
            @PathVariable Long commentId,
            @RequestBody Map<String, String> payload) {
        String currentUsername = getCurrentUsername();
        if (currentUsername == null) {
            return ResponseEntity.status(401).body(ResponseDTO.fail(null, "Vui lòng đăng nhập"));
        }
        String content = payload.get("content");
        return ResponseEntity.ok(ResponseDTO.success(profilePostService.updateComment(commentId, content, currentUsername)));
    }

    @DeleteMapping("/comments/{commentId}")
    public ResponseEntity<ResponseDTO<Void>> deleteComment(@PathVariable Long commentId) {
        String currentUsername = getCurrentUsername();
        if (currentUsername == null) {
            return ResponseEntity.status(401).body(ResponseDTO.fail(null, "Vui lòng đăng nhập"));
        }
        profilePostService.deleteComment(commentId, currentUsername);
        return ResponseEntity.ok(ResponseDTO.success(null));
    }
}
