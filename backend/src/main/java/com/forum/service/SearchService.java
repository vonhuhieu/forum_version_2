package com.forum.service;

import com.forum.dto.PageResponseDTO;
import com.forum.dto.ResponseDTO;
import com.forum.dto.SearchResultDTO;
import com.forum.elasticsearch.document.SearchDocument;
import com.forum.elasticsearch.repository.SearchDocumentRepository;
import com.forum.entity.Post;
import com.forum.entity.Thread;
import com.forum.repository.PostRepository;
import com.forum.repository.ThreadRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.opensearch.data.client.osc.NativeQuery;
import org.opensearch.client.opensearch._types.FieldValue;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.HighlightQuery;
import org.springframework.data.elasticsearch.core.query.highlight.Highlight;
import org.springframework.data.elasticsearch.core.query.highlight.HighlightField;
import org.springframework.data.elasticsearch.core.query.highlight.HighlightParameters;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SearchService {

    private final ThreadRepository threadRepository;
    private final PostRepository postRepository;
    private final SearchDocumentRepository searchDocumentRepository;
    private final ElasticsearchOperations elasticsearchOperations;

    public ResponseDTO<PageResponseDTO<SearchResultDTO>> search(String keyword, String sortBy, int page, int size) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return ResponseDTO.success(new PageResponseDTO<>(Collections.emptyList(), 0, 0L, page, size));
        }

        String trimmedKeyword = keyword.trim();
        boolean canSeeInternal = isUserAuthorizedForInternalThreads();

        // 1. Configure Highlighting
        HighlightParameters parameters = HighlightParameters.builder()
                .withPreTags("<mark class=\"search-highlight\">")
                .withPostTags("</mark>")
                .withFragmentSize(150)
                .withNumberOfFragments(1)
                .build();

        List<HighlightField> highlightFields = List.of(
                new HighlightField("threadTitle"),
                new HighlightField("content")
        );

        HighlightQuery highlightQuery = new HighlightQuery(
                new Highlight(parameters, highlightFields),
                SearchDocument.class
        );

        // 2. Build Native Query for Elasticsearch
        Pageable pageable = PageRequest.of(page, size);
        var queryBuilder = NativeQuery.builder()
                .withQuery(q -> q
                        .bool(b -> {
                            b.must(m -> m
                                    .bool(bInner -> bInner
                                            .should(s1 -> s1
                                                    .bool(b1 -> b1
                                                            .must(m1 -> m1.term(t -> t.field("type").value(FieldValue.of("thread"))))
                                                            .must(m2 -> m2.multiMatch(mm -> mm
                                                                    .query(trimmedKeyword)
                                                                    .fields("threadTitle^3.0", "content^1.0")
                                                            ))
                                                    )
                                            )
                                            .should(s2 -> s2
                                                    .bool(b2 -> b2
                                                            .must(m1 -> m1.term(t -> t.field("type").value(FieldValue.of("post"))))
                                                            .must(m2 -> m2.multiMatch(mm -> mm
                                                                    .query(trimmedKeyword)
                                                                    .fields("content")
                                                            ))
                                                    )
                                            )
                                            .minimumShouldMatch("1")
                                    )
                            ).filter(f -> f
                                    .term(t -> t
                                            .field("active")
                                            .value(FieldValue.of(true))
                                    )
                            );
                            if (!canSeeInternal) {
                                b.mustNot(mn -> mn
                                        .term(t -> t
                                                .field("scope")
                                                .value(FieldValue.of("INTERNAL"))
                                        )
                                );
                            }
                            return b;
                        })
                )
                .withHighlightQuery(highlightQuery)
                .withTrackTotalHits(true)
                .withPageable(pageable);

        if ("date".equalsIgnoreCase(sortBy)) {
            queryBuilder.withSort(Sort.by(Sort.Direction.DESC, "createdAt"));
        }

        NativeQuery query = queryBuilder.build();

        // 3. Execute Search
        SearchHits<SearchDocument> searchHits;
        try {
            searchHits = elasticsearchOperations.search(query, SearchDocument.class);
        } catch (org.springframework.data.elasticsearch.NoSuchIndexException e) {
            log.warn("Tìm kiếm không khả dụng do index 'forum_search' chưa tồn tại: {}", e.getMessage());
            return ResponseDTO.success(new PageResponseDTO<>(Collections.emptyList(), 0, 0L, page, size));
        } catch (Exception e) {
            log.error("Lỗi ngoài dự kiến khi thực thi tìm kiếm với từ khóa '{}': ", keyword, e);
            return ResponseDTO.success(new PageResponseDTO<>(Collections.emptyList(), 0, 0L, page, size));
        }

        // 4. Map Results
        List<SearchResultDTO> results = new ArrayList<>();
        for (SearchHit<SearchDocument> hit : searchHits.getSearchHits()) {
            SearchDocument doc = hit.getContent();
            SearchResultDTO dto = new SearchResultDTO();
            dto.setType(doc.getType());
            dto.setId(doc.getOriginalId());
            dto.setThreadId(doc.getThreadId());
            dto.setCategoryId(doc.getCategoryId());
            dto.setCategoryName(doc.getCategoryName());
            dto.setAuthorName(doc.getAuthorName());
            dto.setCreatedAt(doc.getCreatedAt());

            List<String> titleHighlights = hit.getHighlightField("threadTitle");
            String highlightedTitle = (titleHighlights != null && !titleHighlights.isEmpty()) 
                    ? titleHighlights.get(0) 
                    : doc.getThreadTitle();
            dto.setThreadTitle(highlightedTitle);

            List<String> contentHighlights = hit.getHighlightField("content");
            boolean contentMatched = contentHighlights != null && !contentHighlights.isEmpty();
            dto.setContentMatched(contentMatched);
            String snippet = contentMatched ? contentHighlights.get(0) : "";
            if (snippet.isEmpty() && doc.getContent() != null) {
                String cleanText = doc.getContent().replaceAll("<[^>]*>", " ").replaceAll("\\s+", " ").trim();
                snippet = cleanText.substring(0, Math.min(cleanText.length(), 150));
            }
            dto.setSnippet(snippet);

            if ("post".equals(doc.getType())) {
                dto.setPageNumber(calculatePostPageNumber(doc.getOriginalId(), doc.getThreadId(), doc.getCreatedAt()));
            } else {
                dto.setPageNumber(1);
            }
            results.add(dto);
        }

        long totalElements = searchHits.getTotalHits();
        int maxPages = 10;
        int totalPages = (int) Math.ceil((double) totalElements / size);
        if (totalPages > maxPages) {
            totalPages = maxPages;
        }

        return ResponseDTO.success(new PageResponseDTO<>(results, totalPages, totalElements, page, size));
    }

    @Transactional(readOnly = true)
    public ResponseDTO<Void> reindexAll() {
        try {
            // Xóa và tạo lại index để cập nhật chính xác mapping
            org.springframework.data.elasticsearch.core.IndexOperations indexOps = elasticsearchOperations.indexOps(SearchDocument.class);
            try {
                if (indexOps.exists()) {
                    log.info("Xóa index cũ để chuẩn bị tái lập chỉ mục...");
                    indexOps.delete();
                }
            } catch (Exception e) {
                log.warn("Lưu ý khi xóa index cũ: {}", e.getMessage());
            }
            
            try {
                if (!indexOps.exists()) {
                    log.info("Tạo index mới và áp dụng mapping...");
                    indexOps.create();
                    indexOps.putMapping(indexOps.createMapping(SearchDocument.class));
                }
            } catch (Exception e) {
                log.warn("Lưu ý khi tạo index mới / áp dụng mapping: {}", e.getMessage());
            }

            List<Thread> threads = threadRepository.findAll();
            List<SearchDocument> batch = new ArrayList<>();
            int batchSize = 500;

            for (Thread t : threads) {
                batch.add(mapThreadToSearchDocument(t));
                if (batch.size() >= batchSize) {
                    searchDocumentRepository.saveAll(batch);
                    batch.clear();
                }
            }

            List<Post> posts = postRepository.findAll();
            for (Post p : posts) {
                batch.add(mapPostToSearchDocument(p));
                if (batch.size() >= batchSize) {
                    searchDocumentRepository.saveAll(batch);
                    batch.clear();
                }
            }

            if (!batch.isEmpty()) {
                searchDocumentRepository.saveAll(batch);
            }
            log.info("Reindex hoàn tất thành công (tổng cộng: {} threads, {} posts).", threads.size(), posts.size());
            return ResponseDTO.success(null);
        } catch (Exception e) {
            log.error("Lỗi trong quá trình Reindex OpenSearch: ", e);
            throw new RuntimeException("Reindexing failed: " + e.getMessage(), e);
        }
    }


    public SearchDocument mapThreadToSearchDocument(Thread thread) {
        SearchDocument doc = new SearchDocument();
        doc.setId("thread_" + thread.getId());
        doc.setOriginalId(thread.getId());
        doc.setType("thread");
        doc.setThreadId(thread.getId());
        doc.setThreadTitle(thread.getTitle());
        doc.setContent(thread.getContent());
        doc.setCategoryName(thread.getCategory() != null ? thread.getCategory().getName() : null);
        doc.setCategoryId(thread.getCategory() != null ? thread.getCategory().getId() : null);
        doc.setAuthorName(thread.getAuthor() != null ? (thread.getAuthor().getDisplayName() != null ? thread.getAuthor().getDisplayName() : thread.getAuthor().getUsername()) : "Ẩn danh");
        doc.setCreatedAt(thread.getCreatedAt() != null ? thread.getCreatedAt() : LocalDateTime.now());
        doc.setScope(thread.getScope());
        doc.setActive(thread.isActive());
        return doc;
    }

    public SearchDocument mapPostToSearchDocument(Post post) {
        SearchDocument doc = new SearchDocument();
        doc.setId("post_" + post.getId());
        doc.setOriginalId(post.getId());
        doc.setType("post");
        doc.setThreadId(post.getThread() != null ? post.getThread().getId() : null);
        doc.setThreadTitle(post.getThread() != null ? post.getThread().getTitle() : null);
        doc.setContent(post.getContent());
        doc.setCategoryName((post.getThread() != null && post.getThread().getCategory() != null) ? post.getThread().getCategory().getName() : null);
        doc.setCategoryId((post.getThread() != null && post.getThread().getCategory() != null) ? post.getThread().getCategory().getId() : null);
        doc.setAuthorName(post.getAuthor() != null ? (post.getAuthor().getDisplayName() != null ? post.getAuthor().getDisplayName() : post.getAuthor().getUsername()) : "Ẩn danh");
        doc.setCreatedAt(post.getCreatedAt() != null ? post.getCreatedAt() : LocalDateTime.now());
        doc.setScope(post.getThread() != null ? post.getThread().getScope() : com.forum.utils.Constants.THREAD_SCOPE_PUBLIC);
        doc.setActive(post.getThread() != null ? post.getThread().isActive() : true);
        return doc;
    }

    private boolean isUserAuthorizedForInternalThreads() {
        org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return false;
        }
        return auth.getAuthorities().stream()
                .map(org.springframework.security.core.GrantedAuthority::getAuthority)
                .noneMatch(role -> role.equals(com.forum.utils.Constants.ROLE_NON_OFFICIAL_USER));
    }

    private int calculatePostPageNumber(Long postId, Long threadId, LocalDateTime createdAt) {
        if (threadId == null || createdAt == null) return 1;
        long count = postRepository.countBeforePost(threadId, createdAt, postId);
        long seqNum = count + 2; // +1 for 1-based, +1 for main post
        int size = 10; // items per page matches UI itemsPerPage
        return (int) Math.ceil((double) seqNum / size);
    }
}
