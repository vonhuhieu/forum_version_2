package com.forum.service;

import com.forum.dto.PostDTO;
import com.forum.dto.ResponseDTO;
import com.forum.entity.Post;
import com.forum.entity.Thread;
import com.forum.entity.ThreadSubscription;
import com.forum.entity.User;
import com.forum.mapper.PostMapper;
import com.forum.mapper.LabelMapper;
import com.forum.mapper.CategoryMapper;
import com.forum.repository.PostRepository;
import com.forum.repository.ThreadRepository;
import com.forum.repository.UserRepository;
import com.forum.repository.ThreadSubscriptionRepository;
import com.forum.elasticsearch.repository.SearchDocumentRepository;
import com.forum.elasticsearch.document.SearchDocument;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Transactional
@RequiredArgsConstructor
public class PostService {

    private final PostRepository postRepository;
    private final ThreadRepository threadRepository;
    private final UserRepository userRepository;
    private final PostMapper postMapper;
    private final LabelMapper labelMapper;
    private final CategoryMapper categoryMapper;
    private final NotificationService notificationService;
    private final ReactionService reactionService;
    private final ThreadSubscriptionRepository threadSubscriptionRepository;
    private final SearchDocumentRepository searchDocumentRepository;

    private SearchDocument mapPostToSearchDocument(Post post) {
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
        doc.setCreatedAt(post.getCreatedAt() != null ? post.getCreatedAt() : java.time.LocalDateTime.now());
        doc.setScope(post.getThread() != null ? post.getThread().getScope() : com.forum.utils.Constants.THREAD_SCOPE_PUBLIC);
        doc.setActive(post.getThread() != null ? post.getThread().isActive() : true);
        return doc;
    }

    public static class OffsetLimitPageable implements org.springframework.data.domain.Pageable {
        private final int limit;
        private final long offset;
        private final org.springframework.data.domain.Sort sort;

        public OffsetLimitPageable(long offset, int limit) {
            this.offset = offset;
            this.limit = limit;
            this.sort = org.springframework.data.domain.Sort.unsorted();
        }

        @Override
        public int getPageNumber() { return (int) (offset / limit); }
        @Override
        public int getPageSize() { return limit; }
        @Override
        public long getOffset() { return offset; }
        @Override
        public org.springframework.data.domain.Sort getSort() { return sort; }
        @Override
        public org.springframework.data.domain.Pageable next() { return new OffsetLimitPageable(offset + limit, limit); }
        @Override
        public org.springframework.data.domain.Pageable previousOrFirst() { return new OffsetLimitPageable(Math.max(0, offset - limit), limit); }
        @Override
        public org.springframework.data.domain.Pageable first() { return new OffsetLimitPageable(0, limit); }
        @Override
        public org.springframework.data.domain.Pageable withPage(int pageNumber) { return new OffsetLimitPageable((long) pageNumber * limit, limit); }
        @Override
        public boolean hasPrevious() { return offset > 0; }
    }

    public void evictCache(Long threadId) {
        // Cache disabled because comments are paginated at DB level
    }

    private boolean isUserAuthorizedForInternalThreads() {
        org.springframework.security.core.Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return false;
        }
        return auth.getAuthorities().stream()
                .map(org.springframework.security.core.GrantedAuthority::getAuthority)
                .noneMatch(role -> role.equals(com.forum.utils.Constants.ROLE_NON_OFFICIAL_USER));
    }

    public ResponseDTO<com.forum.dto.PageResponseDTO<PostDTO>> getPostsByThread(Long threadId, int page, int size) {
        Thread thread = threadRepository.findById(threadId)
                .orElseThrow(() -> new RuntimeException("Thread not found"));
        if (com.forum.utils.Constants.THREAD_SCOPE_INTERNAL.equals(thread.getScope()) && !isUserAuthorizedForInternalThreads()) {
            throw new RuntimeException("Access denied");
        }

        long offset;
        int limit;
        if (page == 0) {
            offset = 0;
            limit = size - 1;
        } else {
            offset = (long) page * size - 1;
            limit = size;
        }

        if (limit <= 0) {
            limit = 1;
        }

        org.springframework.data.domain.Page<Post> postPage = postRepository.findByThreadIdOrderByCreatedAtAsc(
                threadId, new OffsetLimitPageable(offset, limit));

        List<PostDTO> dtos = postMapper.toDTOList(postPage.getContent());
        reactionService.enrichPostsWithReactions(dtos, threadId);

        // Populate user-specific data (currentUserReaction) dynamically
        String username = null;
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof String && !principal.equals("anonymousUser")) {
            username = (String) principal;
        }

        if (username != null && !dtos.isEmpty()) {
            List<com.forum.entity.Reaction> userReactions = reactionService.getUserReactionsInThread(username, threadId);
            java.util.Map<Long, com.forum.dto.ReactionIconDTO> reactionMap = new java.util.HashMap<>();
            for (com.forum.entity.Reaction r : userReactions) {
                if (r.getPost() != null && r.getPost().getId() != null && r.getReactionIcon() != null) {
                    reactionMap.put(r.getPost().getId(), reactionService.convertToIconDTO(r.getReactionIcon()));
                }
            }
            for (PostDTO dto : dtos) {
                dto.setCurrentUserReaction(reactionMap.get(dto.getId()));
            }
        }

        com.forum.dto.PageResponseDTO<PostDTO> pageResponse = new com.forum.dto.PageResponseDTO<>(
            dtos,
            (int) Math.ceil((double) (1 + postPage.getTotalElements()) / size),
            postPage.getTotalElements(),
            page,
            size
        );

        return ResponseDTO.success(pageResponse);
    }

    public ResponseDTO<Integer> getPostPageNumber(Long postId, int size) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post not found"));
        long count = postRepository.countBeforePost(post.getThread().getId(), post.getCreatedAt(), post.getId());
        long seqNum = count + 2; // +1 for 1-based, +1 for main post
        int page = (int) Math.ceil((double) seqNum / size);
        return ResponseDTO.success(page);
    }

    public ResponseDTO<com.forum.dto.PageResponseDTO<PostDTO>> getMyPostsPaged(String username, int page, int size) {
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(page, size);
        org.springframework.data.domain.Page<Post> postPage = postRepository.findByAuthorUsernameOrderByCreatedAtDesc(username, pageable);
        
        List<PostDTO> dtos = postPage.getContent().stream().map(post -> {
            PostDTO dto = postMapper.toDTO(post);
            if (post.getThread() != null) {
                dto.setThreadId(post.getThread().getId());
                dto.setThreadTitle(post.getThread().getTitle());
                if (post.getThread().getLabel() != null) {
                    dto.setThreadLabel(labelMapper.toDTO(post.getThread().getLabel()));
                }
                if (post.getThread().getCategory() != null) {
                    dto.setCategory(categoryMapper.toDTO(post.getThread().getCategory()));
                }
                // Tính số thứ tự seqNumber trong thread
                long count = postRepository.countBeforePost(post.getThread().getId(), post.getCreatedAt(), post.getId());
                dto.setSeqNumber(count + 2);
            }
            return dto;
        }).collect(java.util.stream.Collectors.toList());

        return ResponseDTO.success(new com.forum.dto.PageResponseDTO<>(
            dtos,
            postPage.getTotalPages(),
            postPage.getTotalElements(),
            postPage.getNumber(),
            postPage.getSize()
        ));
    }

    private void enrichPosts(List<PostDTO> dtos) {
        if (dtos != null) {
            dtos.forEach(this::enrichPost);
        }
    }

    private void enrichPost(PostDTO dto) {
        if (dto == null || dto.getId() == null) return;
        dto.setReactionSummary(reactionService.getSummaryForPost(dto.getId()));
        dto.setCurrentUserReaction(reactionService.getCurrentUserReactionForPost(dto.getId()));
        dto.setRecentReactors(reactionService.getRecentReactorsForPost(dto.getId()));
    }

    public ResponseDTO<PostDTO> createPost(PostDTO postDTO) {
        if (postDTO.getThreadId() == null) {
            throw new RuntimeException("Thread ID cannot be null");
        }

        Thread thread = threadRepository.findById(postDTO.getThreadId())
                .orElseThrow(() -> new RuntimeException("Thread not found"));

        if (com.forum.utils.Constants.THREAD_SCOPE_INTERNAL.equals(thread.getScope()) && !isUserAuthorizedForInternalThreads()) {
            throw new RuntimeException("Access denied");
        }

        if (thread.isLocked()) {
            throw new RuntimeException("Thread is locked for replies");
        }

        Post post = postMapper.toEntity(postDTO);
        post.setThread(thread);

        // Get username from SecurityContext
        String username = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        userRepository.findByUsername(username).ifPresent(post::setAuthor);

        Post saved = postRepository.save(post);
        evictCache(postDTO.getThreadId());

        // Update thread statistics
        thread.setReplyCount(thread.getReplyCount() + 1);
        thread.setLastPostAt(saved.getCreatedAt());
        Thread updatedThread = threadRepository.save(thread);
        com.forum.service.ThreadService.clearListCache();

        // Kiểm tra điều kiện tự động theo dõi trước ở luồng chính để phản hồi DTO chính xác
        boolean willAutoFollow = false;
        try {
            User actor = saved.getAuthor();
            if (actor != null && updatedThread.getAuthor() != null && !updatedThread.getAuthor().getId().equals(actor.getId())) {
                Optional<ThreadSubscription> subOpt = threadSubscriptionRepository.findByThreadIdAndUserId(updatedThread.getId(), actor.getId());
                if (subOpt.isEmpty() || !subOpt.get().isFollowing()) {
                    willAutoFollow = true;
                }
            }
        } catch (Exception e) {
            // ignore
        }

        final boolean runAutoFollow = willAutoFollow;
        
        // Định nghĩa tác vụ bất đồng bộ
        Runnable asyncTask = () -> {
            // 1. Lưu chỉ mục tìm kiếm
            try {
                searchDocumentRepository.save(mapPostToSearchDocument(saved));
            } catch (Exception e) {
                e.printStackTrace();
            }

            // 2. Lưu trạng thái tự động theo dõi vào DB
            if (runAutoFollow) {
                try {
                    User actor = saved.getAuthor();
                    Optional<ThreadSubscription> subOpt = threadSubscriptionRepository.findByThreadIdAndUserId(updatedThread.getId(), actor.getId());
                    if (subOpt.isEmpty()) {
                        ThreadSubscription sub = new ThreadSubscription();
                        sub.setThread(updatedThread);
                        sub.setUser(actor);
                        sub.setFollowing(true);
                        threadSubscriptionRepository.save(sub);
                    } else {
                        ThreadSubscription sub = subOpt.get();
                        sub.setFollowing(true);
                        threadSubscriptionRepository.save(sub);
                    }
                } catch (Exception e) {
                    // ignore
                }
            }

            // 3. Gửi thông báo (mentions, quotes, followers)
            try {
                User actor = saved.getAuthor();
                String content = saved.getContent();
                
                // Detect tagged users
                Set<Long> mentionedUserIds = notificationService.getMentionedUserIds(actor, content);
                Set<Long> notifiedUserIds = new HashSet<>(mentionedUserIds);
                
                // Send MENTION notifications to all tagged users
                for (Long recipientId : mentionedUserIds) {
                    userRepository.findById(recipientId).ifPresent(recipient -> {
                        notificationService.sendMentionNotification(actor, recipient, updatedThread, saved);
                    });
                }

                // Pattern to detect quotes with data-source
                Pattern pattern = Pattern.compile("data-source=\"([^\"]+)\"");
                Matcher matcher = pattern.matcher(content != null ? content : "");
                Set<String> quotedIds = new HashSet<>();
                while (matcher.find()) {
                    quotedIds.add(matcher.group(1));
                }

                for (String sourceId : quotedIds) {
                    if ("main_thread_entry".equals(sourceId)) {
                        // Quoting the thread itself
                        User threadAuthor = updatedThread.getAuthor();
                        if (threadAuthor != null && actor != null && !threadAuthor.getId().equals(actor.getId())) {
                            // Suppress quote notification if the threadAuthor is already mentioned
                            if (!notifiedUserIds.contains(threadAuthor.getId())) {
                                notificationService.sendQuoteNotification(actor, threadAuthor, updatedThread, saved);
                                notifiedUserIds.add(threadAuthor.getId());
                            }
                        }
                    } else {
                        // Quoting a specific post
                        try {
                            Long postId = Long.parseLong(sourceId);
                            postRepository.findById(postId).ifPresent(quotedPost -> {
                                User quotedAuthor = quotedPost.getAuthor();
                                if (quotedAuthor != null && actor != null && !quotedAuthor.getId().equals(actor.getId())) {
                                    // Suppress quote notification if the quotedAuthor is already mentioned
                                    if (!notifiedUserIds.contains(quotedAuthor.getId())) {
                                        notificationService.sendQuoteNotification(actor, quotedAuthor, updatedThread, saved);
                                        notifiedUserIds.add(quotedAuthor.getId());
                                    }
                                }
                            });
                        } catch (NumberFormatException e) {
                            // ignore invalid IDs
                        }
                    }
                }

                // Notify all thread followers who have NOT been notified yet via MENTION or QUOTE
                Set<Long> followers = new HashSet<>();
                
                // Explicit followers
                List<ThreadSubscription> subs = threadSubscriptionRepository.findByThreadIdAndIsFollowingTrue(updatedThread.getId());
                for (ThreadSubscription sub : subs) {
                    if (sub.getUser() != null) {
                        followers.add(sub.getUser().getId());
                    }
                }

                // Implicit thread owner follow
                User threadOwner = updatedThread.getAuthor();
                if (threadOwner != null) {
                    Optional<ThreadSubscription> ownerSubOpt = threadSubscriptionRepository.findByThreadIdAndUserId(updatedThread.getId(), threadOwner.getId());
                    if (ownerSubOpt.isEmpty() || ownerSubOpt.get().isFollowing()) {
                        followers.add(threadOwner.getId());
                    }
                }

                for (Long followerId : followers) {
                    if (actor != null && followerId.equals(actor.getId())) {
                        continue;
                    }
                    if (notifiedUserIds.contains(followerId)) {
                        continue;
                    }
                    userRepository.findById(followerId).ifPresent(follower -> {
                        notificationService.sendNewCommentNotification(actor, updatedThread, saved, follower);
                    });
                }
            } catch (Exception e) {
                // log error or ignore
            }
        };

        // Đăng ký chạy bất đồng bộ sau khi transaction đã commit thành công
        if (org.springframework.transaction.support.TransactionSynchronizationManager.isSynchronizationActive()) {
            org.springframework.transaction.support.TransactionSynchronizationManager.registerSynchronization(
                new org.springframework.transaction.support.TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        java.util.concurrent.CompletableFuture.runAsync(asyncTask);
                    }
                }
            );
        } else {
            java.util.concurrent.CompletableFuture.runAsync(asyncTask);
        }

        PostDTO resultDto = postMapper.toDTO(saved);
        if (willAutoFollow) {
            resultDto.setAutoFollowed(true);
        }
        
        // Khởi tạo các trường reaction trống cho bình luận mới tạo (tiết kiệm 3 truy vấn SELECT DB)
        resultDto.setReactionSummary(new java.util.ArrayList<>());
        resultDto.setCurrentUserReaction(null);
        resultDto.setRecentReactors(new java.util.ArrayList<>());
        
        return ResponseDTO.success(resultDto);
    }

    public ResponseDTO<PostDTO> updatePost(Long id, PostDTO postDTO) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Post not found"));

        // Lấy username từ SecurityContext để kiểm tra quyền sở hữu
        String currentUsername = (String) org.springframework.security.core.context.SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();
        
        if (post.getAuthor() == null || !post.getAuthor().getUsername().equals(currentUsername)) {
            throw new RuntimeException("You are not authorized to edit this post");
        }

        post.setContent(postDTO.getContent());
        post.setAttachedImages(postDTO.getAttachedImages());

        Post saved = postRepository.save(post);
        com.forum.service.ThreadService.clearListCache();
        if (saved.getThread() != null) {
            evictCache(saved.getThread().getId());
        }
        try {
            searchDocumentRepository.save(mapPostToSearchDocument(saved));
        } catch (Exception e) {
            e.printStackTrace();
        }
        PostDTO resultDto = postMapper.toDTO(saved);
        enrichPost(resultDto);
        return ResponseDTO.success(resultDto);
    }
}
