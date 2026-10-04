package com.forum.controller;

import com.forum.dto.PollDTO;
import com.forum.dto.ResponseDTO;
import com.forum.service.PollService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/polls")
@RequiredArgsConstructor
public class PollController {

    private final PollService pollService;

    @PostMapping("/{pollId}/vote")
    public ResponseEntity<ResponseDTO<PollDTO>> vote(
            @PathVariable Long pollId,
            @RequestBody Map<String, List<Long>> body
    ) {
        try {
            List<Long> optionIds = body.get("optionIds");
            PollDTO result = pollService.vote(pollId, optionIds);
            return ResponseEntity.ok(ResponseDTO.success(result));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ResponseDTO.fail(null));
        }
    }

    @GetMapping("/{pollId}/votes")
    public ResponseEntity<ResponseDTO<org.springframework.data.domain.Page<com.forum.dto.PollVoteDetailDTO>>> getPollVotes(
            @PathVariable Long pollId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long optionId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        try {
            org.springframework.data.domain.Page<com.forum.dto.PollVoteDetailDTO> result = 
                pollService.getPollVotes(pollId, keyword, optionId, page, size);
            return ResponseEntity.ok(ResponseDTO.success(result));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ResponseDTO.fail(null));
        }
    }
}
