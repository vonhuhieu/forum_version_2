package com.forum.controller;

import com.forum.dto.PageResponseDTO;
import com.forum.dto.ResponseDTO;
import com.forum.dto.SearchResultDTO;
import com.forum.service.SearchService;
import com.forum.service.SearchHistoryService;
import com.forum.utils.Constants;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
public class SearchController {

    private final SearchService searchService;
    private final SearchHistoryService searchHistoryService;

    @Value("${app.security.local-ips}")
    private String localIpsConfig;

    @GetMapping
    public ResponseEntity<ResponseDTO<PageResponseDTO<SearchResultDTO>>> search(
            @RequestParam(name = "q", required = false) String keyword,
            @RequestParam(name = "sortBy", defaultValue = "relevance") String sortBy,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size) {
        
        // Tự động lưu lịch sử tìm kiếm khi user đã đăng nhập
        org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            String username = (String) auth.getPrincipal();
            if (keyword != null && !keyword.trim().isEmpty() && page == 0) {
                searchHistoryService.saveSearchKeyword(username, keyword);
            }
        }
        
        return ResponseEntity.ok(searchService.search(keyword, sortBy, page, size));
    }

    @GetMapping("/history")
    public ResponseEntity<ResponseDTO<List<String>>> getHistory() {
        org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return ResponseEntity.ok(ResponseDTO.success(List.of()));
        }
        String username = (String) auth.getPrincipal();
        return ResponseEntity.ok(ResponseDTO.success(searchHistoryService.getRecentKeywords(username)));
    }

    @DeleteMapping("/history")
    public ResponseEntity<ResponseDTO<Void>> deleteHistory(@RequestParam("keyword") String keyword) {
        org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            String username = (String) auth.getPrincipal();
            searchHistoryService.deleteKeyword(username, keyword);
        }
        return ResponseEntity.ok(ResponseDTO.success(null));
    }

    @DeleteMapping("/history/clear")
    public ResponseEntity<ResponseDTO<Void>> clearHistory() {
        org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            String username = (String) auth.getPrincipal();
            searchHistoryService.clearHistory(username);
        }
        return ResponseEntity.ok(ResponseDTO.success(null));
    }

    @PostMapping("/history/sync")
    public ResponseEntity<ResponseDTO<Void>> syncHistory(@RequestBody List<String> keywords) {
        org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            String username = (String) auth.getPrincipal();
            searchHistoryService.syncSearchKeywords(username, keywords);
        }
        return ResponseEntity.ok(ResponseDTO.success(null));
    }

    @PostMapping("/reindex")
    public ResponseEntity<ResponseDTO<Void>> reindex(jakarta.servlet.http.HttpServletRequest request) {
        String remoteAddr = request.getRemoteAddr();
        List<String> allowedIps = Arrays.asList(localIpsConfig.split(","));
        boolean isLocal = allowedIps.contains(remoteAddr);

        if (!isLocal) {
            org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
                return ResponseEntity.status(401).body(ResponseDTO.fail(null, "Yêu cầu đăng nhập"));
            }
            boolean isAdmin = auth.getAuthorities().stream()
                    .map(org.springframework.security.core.GrantedAuthority::getAuthority)
                    .anyMatch(role -> role.equals(Constants.ROLE_ADMIN) || role.equals(Constants.ROLE_SUPER_ADMIN));
            if (!isAdmin) {
                return ResponseEntity.status(403).body(ResponseDTO.fail(null, "Không có quyền thực hiện tác vụ này"));
            }
        }

        return ResponseEntity.ok(searchService.reindexAll());
    }
}
