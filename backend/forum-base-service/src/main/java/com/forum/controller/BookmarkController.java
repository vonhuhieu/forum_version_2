package com.forum.controller;

import com.forum.dto.BookmarkDTO;
import com.forum.dto.BookmarkRequestDTO;
import com.forum.dto.ResponseDTO;
import com.forum.service.BookmarkService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/bookmarks")
@RequiredArgsConstructor
public class BookmarkController {

    private final BookmarkService bookmarkService;

    /**
     * Tạo bookmark mới
     */
    @PostMapping
    public ResponseEntity<ResponseDTO<BookmarkDTO>> createBookmark(@RequestBody BookmarkRequestDTO request) {
        return ResponseEntity.ok(bookmarkService.createBookmark(request));
    }

    /**
     * Cập nhật bookmark (note, labels)
     */
    @PutMapping("/{id}")
    public ResponseEntity<ResponseDTO<BookmarkDTO>> updateBookmark(
            @PathVariable Long id,
            @RequestBody BookmarkRequestDTO request) {
        return ResponseEntity.ok(bookmarkService.updateBookmark(id, request));
    }

    /**
     * Xóa bookmark
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ResponseDTO<Void>> deleteBookmark(@PathVariable Long id) {
        return ResponseEntity.ok(bookmarkService.deleteBookmark(id));
    }

    /**
     * Lấy danh sách bookmark phân trang, hỗ trợ lọc theo label
     */
    @GetMapping
    public ResponseEntity<ResponseDTO<Page<BookmarkDTO>>> getMyBookmarks(
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            @RequestParam(required = false) String label) {
        return ResponseEntity.ok(bookmarkService.getMyBookmarks(pageable, label));
    }

    /**
     * Lấy tất cả labels không trùng lặp của user hiện tại
     */
    @GetMapping("/labels")
    public ResponseEntity<ResponseDTO<List<String>>> getMyLabels() {
        return ResponseEntity.ok(bookmarkService.getMyLabels());
    }

    /**
     * Kiểm tra hàng loạt trạng thái bookmark cho danh sách thread/post IDs
     */
    @PostMapping("/check-status")
    public ResponseEntity<ResponseDTO<Map<String, Long>>> checkBookmarkStatus(
            @RequestBody Map<String, List<Long>> request) {
        List<Long> threadIds = request.get("threadIds");
        List<Long> postIds = request.get("postIds");
        return ResponseEntity.ok(bookmarkService.checkBookmarkStatus(threadIds, postIds));
    }

    /**
     * Lấy thông tin 1 bookmark cụ thể
     */
    @GetMapping("/{id}")
    public ResponseEntity<ResponseDTO<BookmarkDTO>> getBookmarkById(@PathVariable Long id) {
        return ResponseEntity.ok(bookmarkService.getBookmarkById(id));
    }
}
