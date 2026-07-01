package com.forum.controller;

import com.forum.dto.PostDTO;
import com.forum.dto.ResponseDTO;
import com.forum.service.PostService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;

    @GetMapping("/thread/{threadId}")
    public ResponseEntity<ResponseDTO<com.forum.dto.PageResponseDTO<PostDTO>>> getPostsByThread(
            @PathVariable Long threadId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(postService.getPostsByThread(threadId, page, size));
    }

    @GetMapping("/{id}/page-number")
    public ResponseEntity<ResponseDTO<Integer>> getPostPageNumber(
            @PathVariable Long id,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(postService.getPostPageNumber(id, size));
    }

    @GetMapping("/me")
    public ResponseEntity<ResponseDTO<com.forum.dto.PageResponseDTO<PostDTO>>> getMyPosts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        String currentUsername = (String) org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return ResponseEntity.ok(postService.getMyPostsPaged(currentUsername, page, size));
    }

    @PostMapping
    public ResponseEntity<ResponseDTO<PostDTO>> createPost(@RequestBody PostDTO postDTO) {
        try {
            return ResponseEntity.ok(postService.createPost(postDTO));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<ResponseDTO<PostDTO>> updatePost(@PathVariable Long id, @RequestBody PostDTO postDTO) {
        try {
            return ResponseEntity.ok(postService.updatePost(id, postDTO));
        } catch (RuntimeException e) {
            return ResponseEntity.status(403).build();
        }
    }
}
