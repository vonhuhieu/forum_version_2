package com.forum.controller;

import com.forum.dto.ResponseDTO;
import com.forum.service.UserFollowService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user-follows")
@RequiredArgsConstructor
public class UserFollowController {

    private final UserFollowService userFollowService;

    @GetMapping("/status/{username}")
    public ResponseEntity<ResponseDTO<Boolean>> getFollowStatus(@PathVariable String username) {
        return ResponseEntity.ok(userFollowService.getFollowStatus(username));
    }

    @PostMapping("/{username}")
    public ResponseEntity<ResponseDTO<Void>> setFollowStatus(
            @PathVariable String username,
            @RequestParam boolean following) {
        return ResponseEntity.ok(userFollowService.setFollowStatus(username, following));
    }
}
