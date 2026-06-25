package com.forum.controller;

import com.forum.dto.PageResponseDTO;
import com.forum.dto.ResponseDTO;
import com.forum.dto.SearchResultDTO;
import com.forum.service.SearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
public class SearchController {

    private final SearchService searchService;

    @GetMapping
    public ResponseEntity<ResponseDTO<PageResponseDTO<SearchResultDTO>>> search(
            @RequestParam(name = "q", required = false) String keyword,
            @RequestParam(name = "sortBy", defaultValue = "relevance") String sortBy,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size) {
        return ResponseEntity.ok(searchService.search(keyword, sortBy, page, size));
    }

    @PostMapping("/reindex")
    public ResponseEntity<ResponseDTO<Void>> reindex() {
        return ResponseEntity.ok(searchService.reindexAll());
    }
}
