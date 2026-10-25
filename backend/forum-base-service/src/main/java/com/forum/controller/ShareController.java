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

    @GetMapping(value = "/thread/{id:\\d+}", produces = MediaType.TEXT_HTML_VALUE + ";charset=UTF-8")
    public ResponseEntity<String> getSharePage(
            @PathVariable Long id,
            @RequestParam(required = false) String postId,
            HttpServletRequest request) {
        return ResponseEntity.ok(shareService.generateShareHtml(id, postId, request));
    }

    @GetMapping(value = "/tts", produces = "audio/mpeg")
    public ResponseEntity<byte[]> getTtsAudio(@RequestParam("q") String q) {
        byte[] audioBytes = shareService.fetchGoogleTts(q);
        if (audioBytes.length == 0) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok()
                .header(org.springframework.http.HttpHeaders.CACHE_CONTROL, "public, max-age=86400")
                .header("Access-Control-Allow-Origin", "*")
                .body(audioBytes);
    }
}
