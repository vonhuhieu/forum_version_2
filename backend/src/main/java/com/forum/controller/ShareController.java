package com.forum.controller;

import com.forum.service.ShareService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping({"/api/share", "/share"})
@RequiredArgsConstructor
public class ShareController {

    private final ShareService shareService;

    @GetMapping(value = "/thread/{id}", produces = MediaType.TEXT_HTML_VALUE + ";charset=UTF-8")
    public ResponseEntity<String> getSharePage(
            @PathVariable Long id,
            @RequestParam(required = false) String postId,
            HttpServletRequest request) {
        return ResponseEntity.ok(shareService.generateShareHtml(id, postId, request));
    }
}
