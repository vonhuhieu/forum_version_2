package com.forum.controller;

import com.forum.dto.ResponseDTO;
import com.forum.dto.ThreadDTO;
import com.forum.service.ThreadService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/threads")
@RequiredArgsConstructor
public class ThreadController {

    private final ThreadService threadService;

    @GetMapping
    public ResponseEntity<ResponseDTO<?>> getAllThreads(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long labelId,
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortOrder) {
        if (page != null && size != null) {
            return ResponseEntity.ok(threadService.getAllThreadsPaged(categoryId, labelId, keyword, sortBy, sortOrder, page, size));
        }
        return ResponseEntity.ok(threadService.getAllThreads(categoryId, labelId, limit));
    }

    @GetMapping("/me")
    public ResponseEntity<ResponseDTO<com.forum.dto.PageResponseDTO<ThreadDTO>>> getMyThreads(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        String currentUsername = (String) org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return ResponseEntity.ok(threadService.getMyThreadsPaged(currentUsername, page, size));
    }


    @GetMapping("/latest")
    public ResponseEntity<ResponseDTO<List<ThreadDTO>>> getLatestThreads() {
        return ResponseEntity.ok(threadService.getLatestThreads());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ResponseDTO<ThreadDTO>> getThreadById(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(threadService.getThreadById(id));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping
    public ResponseEntity<ResponseDTO<ThreadDTO>> createThread(@RequestBody ThreadDTO threadDTO) {
        return ResponseEntity.ok(threadService.createThread(threadDTO));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ResponseDTO<ThreadDTO>> updateThread(@PathVariable Long id, @RequestBody ThreadDTO threadDTO) {
        try {
            return ResponseEntity.ok(threadService.updateThread(id, threadDTO));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ResponseDTO<Void>> deleteThread(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(threadService.deleteThread(id));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PatchMapping("/{id}/pin")
    public ResponseEntity<ResponseDTO<ThreadDTO>> togglePin(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(threadService.togglePin(id));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/{id}/follow-status")
    public ResponseEntity<ResponseDTO<Boolean>> getFollowStatus(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(threadService.getFollowStatus(id));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/{id}/follow")
    public ResponseEntity<ResponseDTO<Void>> setFollowStatus(@PathVariable Long id, @RequestParam boolean following) {
        try {
            return ResponseEntity.ok(threadService.setFollowStatus(id, following));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }
}
