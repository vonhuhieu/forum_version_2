package com.forum.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.forum.dto.BookmarkDTO;
import com.forum.dto.BookmarkRequestDTO;
import com.forum.dto.ResponseDTO;
import com.forum.entity.Bookmark;
import com.forum.entity.Post;
import com.forum.entity.Thread;
import com.forum.entity.User;
import com.forum.repository.BookmarkRepository;
import com.forum.repository.PostRepository;
import com.forum.repository.ThreadRepository;
import com.forum.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class BookmarkService {

    private final BookmarkRepository bookmarkRepository;
    private final UserRepository userRepository;
    private final ThreadRepository threadRepository;
    private final PostRepository postRepository;
    private final UserTitleService userTitleService;
    private final ObjectMapper objectMapper;

    private static final int CONTENT_PREVIEW_MAX_LENGTH = 200;

    /**
     * Tạo bookmark mới. Kiểm tra trùng lặp trước khi tạo.
     */
    public ResponseDTO<BookmarkDTO> createBookmark(BookmarkRequestDTO request) {
        String username = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));

        Post post = null;
        if (request.getPostId() != null) {
            post = postRepository.findById(request.getPostId())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy bài viết"));
        }

        Thread thread = null;
        if (request.getThreadId() != null) {
            thread = threadRepository.findById(request.getThreadId())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy chủ đề"));
        } else if (post != null && post.getThread() != null) {
            thread = post.getThread();
        }

        if (thread == null) {
            throw new RuntimeException("Không tìm thấy chủ đề liên quan");
        }

        // Kiểm tra trùng: user đã bookmark cùng thread/post này chưa
        if (post != null) {
            if (bookmarkRepository.existsByUserAndPost(user, post)) {
                // Đã tồn tại - trả về bookmark hiện có thay vì lỗi
                Bookmark existing = bookmarkRepository.findByUserAndPost(user, post).orElse(null);
                return ResponseDTO.success(toDTO(existing));
            }
        } else {
            if (bookmarkRepository.existsByUserAndThreadAndPostIsNull(user, thread)) {
                Bookmark existing = bookmarkRepository.findByUserAndThreadAndPostIsNull(user, thread).orElse(null);
                return ResponseDTO.success(toDTO(existing));
            }
        }

        Bookmark bookmark = new Bookmark();
        bookmark.setUser(user);
        bookmark.setThread(thread);
        bookmark.setPost(post);
        bookmark.setNote(request.getNote());
        bookmark.setLabels(toLabelsJson(request.getLabels()));

        bookmark = bookmarkRepository.save(bookmark);
        return ResponseDTO.success(toDTO(bookmark));
    }

    /**
     * Cập nhật note/labels của bookmark. Kiểm tra quyền sở hữu.
     */
    public ResponseDTO<BookmarkDTO> updateBookmark(Long id, BookmarkRequestDTO request) {
        String username = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));

        Bookmark bookmark = bookmarkRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bookmark"));

        // Kiểm tra quyền sở hữu: bookmark phải thuộc về user hiện tại
        if (!bookmark.getUser().getId().equals(user.getId())) {
            return ResponseDTO.fail(null, "Bạn không có quyền thao tác trên bookmark này");
        }

        bookmark.setNote(request.getNote());
        bookmark.setLabels(toLabelsJson(request.getLabels()));

        bookmark = bookmarkRepository.save(bookmark);
        return ResponseDTO.success(toDTO(bookmark));
    }

    /**
     * Xóa bookmark. Kiểm tra quyền sở hữu.
     */
    public ResponseDTO<Void> deleteBookmark(Long id) {
        String username = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));

        Bookmark bookmark = bookmarkRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bookmark"));

        // Kiểm tra quyền sở hữu
        if (!bookmark.getUser().getId().equals(user.getId())) {
            return ResponseDTO.fail(null, "Bạn không có quyền thao tác trên bookmark này");
        }

        bookmarkRepository.delete(bookmark);
        return ResponseDTO.success(null);
    }

    /**
     * Lấy danh sách bookmark phân trang. Hỗ trợ lọc theo label.
     */
    @Transactional(readOnly = true)
    public ResponseDTO<Page<BookmarkDTO>> getMyBookmarks(Pageable pageable, String label) {
        String username = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));

        Page<Bookmark> bookmarkPage;
        if (label != null && !label.trim().isEmpty()) {
            bookmarkPage = bookmarkRepository.findByUserAndLabel(user, label.trim(), pageable);
        } else {
            bookmarkPage = bookmarkRepository.findByUserOrderByCreatedAtDesc(user, pageable);
        }

        Page<BookmarkDTO> dtoPage = bookmarkPage.map(this::toDTO);
        return ResponseDTO.success(dtoPage);
    }

    /**
     * Lấy tất cả labels không trùng lặp của user hiện tại.
     */
    @Transactional(readOnly = true)
    public ResponseDTO<List<String>> getMyLabels() {
        String username = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));

        List<String> rawLabels = bookmarkRepository.findAllLabelsByUserId(user.getId());

        // Parse từng JSON string và gom vào Set để loại bỏ trùng lặp
        Set<String> uniqueLabels = new TreeSet<>();
        for (String raw : rawLabels) {
            List<String> parsed = parseLabelsJson(raw);
            uniqueLabels.addAll(parsed);
        }

        return ResponseDTO.success(new ArrayList<>(uniqueLabels));
    }

    /**
     * Kiểm tra hàng loạt trạng thái bookmark cho danh sách thread/post IDs.
     * Trả về Map<String, Long> với key = "thread_X" hoặc "post_X", value = bookmarkId.
     */
    @Transactional(readOnly = true)
    public ResponseDTO<Map<String, Long>> checkBookmarkStatus(List<Long> threadIds, List<Long> postIds) {
        String username = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));

        Map<String, Long> statusMap = new HashMap<>();

        // Check bookmark bài gốc (post = null)
        if (threadIds != null && !threadIds.isEmpty()) {
            List<Bookmark> threadBookmarks = bookmarkRepository.findByUserAndThreadIdInAndPostIsNull(user, threadIds);
            for (Bookmark b : threadBookmarks) {
                statusMap.put("thread_" + b.getThread().getId(), b.getId());
            }
        }

        // Check bookmark reply post
        if (postIds != null && !postIds.isEmpty()) {
            List<Bookmark> postBookmarks = bookmarkRepository.findByUserAndPostIdIn(user, postIds);
            for (Bookmark b : postBookmarks) {
                statusMap.put("post_" + b.getPost().getId(), b.getId());
            }
        }

        return ResponseDTO.success(statusMap);
    }

    /**
     * Lấy thông tin 1 bookmark cụ thể.
     */
    @Transactional(readOnly = true)
    public ResponseDTO<BookmarkDTO> getBookmarkById(Long id) {
        String username = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));

        Bookmark bookmark = bookmarkRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bookmark"));

        if (!bookmark.getUser().getId().equals(user.getId())) {
            return ResponseDTO.fail(null, "Bạn không có quyền xem bookmark này");
        }

        return ResponseDTO.success(toDTO(bookmark));
    }

    // ===================== Helper Methods =====================

    /**
     * Chuyển đổi Entity Bookmark sang BookmarkDTO
     */
    private BookmarkDTO toDTO(Bookmark bookmark) {
        if (bookmark == null) return null;

        BookmarkDTO dto = new BookmarkDTO();
        dto.setId(bookmark.getId());
        dto.setThreadId(bookmark.getThread().getId());
        dto.setThreadTitle(bookmark.getThread().getTitle());
        dto.setCreatedAt(bookmark.getCreatedAt());
        dto.setNote(bookmark.getNote());
        dto.setLabels(parseLabelsJson(bookmark.getLabels()));

        // Xác định loại bookmark và thông tin tác giả
        User author;
        String rawContent;
        if (bookmark.getPost() != null) {
            // Bookmark reply post
            dto.setType("POST");
            dto.setPostId(bookmark.getPost().getId());
            author = bookmark.getPost().getAuthor();
            rawContent = bookmark.getPost().getContent();
        } else {
            // Bookmark bài gốc (thread)
            dto.setType("THREAD");
            dto.setPostId(null);
            author = bookmark.getThread().getAuthor();
            rawContent = bookmark.getThread().getContent();
        }

        // Thông tin tác giả
        if (author != null) {
            dto.setAuthorId(author.getId());
            dto.setAuthorUsername(author.getUsername());
            dto.setAuthorDisplayName(author.getDisplayName());
            dto.setAuthorAvatar(author.getAvatar());
            dto.setAuthorIsVerifiedBadge(userTitleService.isVerifiedBadge(author, null));
        }

        // Nội dung preview: ưu tiên note người dùng nhập, nếu không có thì lấy từ thread/post content
        String previewSource = (bookmark.getNote() != null && !bookmark.getNote().trim().isEmpty())
                ? bookmark.getNote()
                : rawContent;
        dto.setContentPreview(truncateContent(stripHtml(previewSource)));

        return dto;
    }

    /**
     * Strip HTML tags từ rich-text content
     */
    private String stripHtml(String html) {
        if (html == null || html.isEmpty()) return "";
        // Loại bỏ tất cả HTML tags
        String text = html.replaceAll("<[^>]*>", "");
        // Decode HTML entities thường gặp
        text = text.replace("&nbsp;", " ")
                   .replace("&amp;", "&")
                   .replace("&lt;", "<")
                   .replace("&gt;", ">")
                   .replace("&quot;", "\"")
                   .replace("&#39;", "'");
        // Chuẩn hóa khoảng trắng
        text = text.replaceAll("\\s+", " ").trim();
        return text;
    }

    /**
     * Cắt ngắn nội dung nếu quá dài
     */
    private String truncateContent(String content) {
        if (content == null || content.isEmpty()) return "";
        if (content.length() <= CONTENT_PREVIEW_MAX_LENGTH) return content;
        return content.substring(0, CONTENT_PREVIEW_MAX_LENGTH) + "...";
    }

    /**
     * Chuyển List<String> labels thành JSON string để lưu vào DB
     */
    private String toLabelsJson(List<String> labels) {
        if (labels == null || labels.isEmpty()) return "[]";
        try {
            return objectMapper.writeValueAsString(labels);
        } catch (JsonProcessingException e) {
            log.error("Error serializing labels to JSON", e);
            return "[]";
        }
    }

    /**
     * Parse JSON string labels thành List<String>
     */
    private List<String> parseLabelsJson(String json) {
        if (json == null || json.isEmpty() || "[]".equals(json)) return new ArrayList<>();
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (JsonProcessingException e) {
            log.error("Error parsing labels JSON: {}", json, e);
            return new ArrayList<>();
        }
    }
}
