package com.forum.controller;

import com.forum.dto.ResponseDTO;
import com.forum.service.ReactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/reactions")
@RequiredArgsConstructor
public class ReactionController {

    private final ReactionService reactionService;

    @PostMapping("/threads/{id}")
    public ResponseEntity<ResponseDTO<Void>> reactToThread(@PathVariable Long id, @RequestParam Long iconId) {
        String username = (String) org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        boolean canSeeInternal = reactionService.isUserAuthorizedForInternalThreads();
        reactionService.reactToThreadAsync(username, id, iconId, canSeeInternal);
        return ResponseEntity.ok(ResponseDTO.success(null));
    }

    @DeleteMapping("/threads/{id}")
    public ResponseEntity<ResponseDTO<Void>> removeReactionFromThread(@PathVariable Long id) {
        String username = (String) org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        reactionService.removeReactionFromThreadAsync(username, id);
        return ResponseEntity.ok(ResponseDTO.success(null));
    }

    @PostMapping("/posts/{id}")
    public ResponseEntity<ResponseDTO<Void>> reactToPost(@PathVariable Long id, @RequestParam Long iconId) {
        String username = (String) org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        boolean canSeeInternal = reactionService.isUserAuthorizedForInternalThreads();
        reactionService.reactToPostAsync(username, id, iconId, canSeeInternal);
        return ResponseEntity.ok(ResponseDTO.success(null));
    }

    @DeleteMapping("/posts/{id}")
    public ResponseEntity<ResponseDTO<Void>> removeReactionFromPost(@PathVariable Long id) {
        String username = (String) org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        reactionService.removeReactionFromPostAsync(username, id);
        return ResponseEntity.ok(ResponseDTO.success(null));
    }

    @GetMapping("/threads/{id}/participants")
    public ResponseEntity<ResponseDTO<org.springframework.data.domain.Page<com.forum.dto.ReactionParticipantDTO>>> getThreadReactionParticipants(
            @PathVariable Long id,
            @RequestParam(required = false) Long iconId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(page, size, org.springframework.data.domain.Sort.by("updatedAt").descending());
        return ResponseEntity.ok(ResponseDTO.success(reactionService.getThreadReactionParticipants(id, iconId, pageable)));
    }

    @GetMapping("/posts/{id}/participants")
    public ResponseEntity<ResponseDTO<org.springframework.data.domain.Page<com.forum.dto.ReactionParticipantDTO>>> getPostReactionParticipants(
            @PathVariable Long id,
            @RequestParam(required = false) Long iconId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(page, size, org.springframework.data.domain.Sort.by("updatedAt").descending());
        return ResponseEntity.ok(ResponseDTO.success(reactionService.getPostReactionParticipants(id, iconId, pageable)));
    }

    @PostMapping("/messages/{id}")
    public ResponseEntity<ResponseDTO<Void>> reactToMessage(@PathVariable Long id, @RequestParam Long iconId) {
        String username = (String) org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        reactionService.reactToMessageAsync(username, id, iconId);
        return ResponseEntity.ok(ResponseDTO.success(null));
    }

    @DeleteMapping("/messages/{id}")
    public ResponseEntity<ResponseDTO<Void>> removeReactionFromMessage(@PathVariable Long id) {
        String username = (String) org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        reactionService.removeReactionFromMessageAsync(username, id);
        return ResponseEntity.ok(ResponseDTO.success(null));
    }

    @GetMapping("/messages/{id}/participants")
    public ResponseEntity<ResponseDTO<org.springframework.data.domain.Page<com.forum.dto.ReactionParticipantDTO>>> getMessageReactionParticipants(
            @PathVariable Long id,
            @RequestParam(required = false) Long iconId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(page, size, org.springframework.data.domain.Sort.by("updatedAt").descending());
        return ResponseEntity.ok(ResponseDTO.success(reactionService.getMessageReactionParticipants(id, iconId, pageable)));
    }

    @GetMapping("/received/summary")
    public ResponseEntity<ResponseDTO<List<com.forum.dto.ReactionSummaryDTO>>> getReceivedReactionsSummary() {
        String username = (String) org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (username == null || username.equals("anonymousUser")) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(ResponseDTO.success(reactionService.getReceivedReactionsSummary()));
    }

    @GetMapping("/received")
    public ResponseEntity<ResponseDTO<org.springframework.data.domain.Page<com.forum.dto.ReceivedReactionDTO>>> getReceivedReactions(
            @RequestParam(required = false) Long iconId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        String username = (String) org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (username == null || username.equals("anonymousUser")) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.UNAUTHORIZED).build();
        }
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(page, size);
        return ResponseEntity.ok(ResponseDTO.success(reactionService.getReceivedReactions(iconId, pageable)));
    }
}
