package com.forum.controller;

import com.forum.dto.ResponseDTO;
import com.forum.entity.User;
import com.forum.entity.UserTitle;
import com.forum.service.UserTitleService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/titles")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
public class UserTitleController {

    private final UserTitleService userTitleService;

    @GetMapping
    public ResponseEntity<ResponseDTO<List<UserTitle>>> getAllTitles() {
        return ResponseEntity.ok(ResponseDTO.success(userTitleService.getAllTitles()));
    }

    @PostMapping
    public ResponseEntity<ResponseDTO<UserTitle>> createTitle(@RequestBody UserTitle title) {
        return ResponseEntity.ok(ResponseDTO.success(userTitleService.createTitle(title)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ResponseDTO<UserTitle>> updateTitle(@PathVariable Long id, @RequestBody UserTitle title) {
        return ResponseEntity.ok(ResponseDTO.success(userTitleService.updateTitle(id, title)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ResponseDTO<Void>> deleteTitle(@PathVariable Long id) {
        userTitleService.deleteTitle(id);
        return ResponseEntity.ok(ResponseDTO.success(null));
    }

    @Data
    public static class AssignTitleRequest {
        private Long titleId; // null để bỏ gán trực tiếp
    }

    @PostMapping("/users/{userId}/assign")
    public ResponseEntity<ResponseDTO<User>> assignTitleToUser(
            @PathVariable Long userId,
            @RequestBody AssignTitleRequest request) {
        User updatedUser = userTitleService.assignTitleToUser(userId, request.getTitleId());
        return ResponseEntity.ok(ResponseDTO.success(updatedUser));
    }
}
