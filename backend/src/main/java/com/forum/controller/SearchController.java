package com.forum.controller;

import com.forum.dto.PageResponseDTO;
import com.forum.dto.ResponseDTO;
import com.forum.dto.SearchResultDTO;
import com.forum.service.SearchService;
import com.forum.utils.Constants;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
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

    @Value("${app.security.local-ips}")
    private String localIpsConfig;

    @GetMapping
    public ResponseEntity<ResponseDTO<PageResponseDTO<SearchResultDTO>>> search(
            @RequestParam(name = "q", required = false) String keyword,
            @RequestParam(name = "sortBy", defaultValue = "relevance") String sortBy,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size) {
        return ResponseEntity.ok(searchService.search(keyword, sortBy, page, size));
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
