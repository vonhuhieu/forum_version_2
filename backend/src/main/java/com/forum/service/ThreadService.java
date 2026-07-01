package com.forum.service;

import com.forum.dto.ResponseDTO;
import com.forum.dto.ThreadDTO;
import com.forum.entity.Thread;
import com.forum.entity.ThreadSubscription;
import com.forum.entity.User;
import com.forum.entity.Post;
import com.forum.mapper.ThreadMapper;
import com.forum.repository.NotificationRepository;
import com.forum.repository.ThreadRepository;
import com.forum.repository.UserRepository;
import com.forum.repository.ThreadSubscriptionRepository;
import com.forum.elasticsearch.repository.SearchDocumentRepository;
import com.forum.elasticsearch.document.SearchDocument;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
@RequiredArgsConstructor
public class ThreadService {

    private final ThreadRepository threadRepository;
    private final UserRepository userRepository;
    private final ThreadMapper threadMapper;
    private final com.forum.repository.PollVoteRepository pollVoteRepository;
    private final com.forum.repository.PostRepository postRepository;
    private final NotificationRepository notificationRepository;
    private final ReactionService reactionService;
    private final NotificationService notificationService;
    private final ThreadSubscriptionRepository threadSubscriptionRepository;
    private final ThreadViewIncrementer threadViewIncrementer;
    private final SearchDocumentRepository searchDocumentRepository;

    private static final java.util.Map<Long, ThreadDTO> threadCache = new java.util.concurrent.ConcurrentHashMap<>();
    private static final java.util.Map<String, List<ThreadDTO>> threadListCache = new java.util.concurrent.ConcurrentHashMap<>();

    public static void clearListCache() {
        threadListCache.clear();
    }

    public static void clearAllCaches() {
        threadCache.clear();
        threadListCache.clear();
    }

    private SearchDocument mapThreadToSearchDocument(Thread thread) {
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
        doc.setCreatedAt(thread.getCreatedAt() != null ? thread.getCreatedAt() : java.time.LocalDateTime.now());
        doc.setScope(thread.getScope());
        doc.setActive(thread.isActive());
        return doc;
    }

    public void evictCache(Long id) {
        if (id != null) {
            threadCache.remove(id);
        }
    }

    public ResponseDTO<List<ThreadDTO>> getAllThreads(Long categoryId, Integer limit) {
        boolean canSeeInternal = isUserAuthorizedForInternalThreads();
        String cacheKey = (categoryId != null ? categoryId.toString() : "null") + "_" + 
                           (limit != null ? limit.toString() : "null") + "_" + 
                           canSeeInternal;
        
        List<ThreadDTO> cached = threadListCache.get(cacheKey);
        if (cached != null) {
            return ResponseDTO.success(cached);
        }

        List<Thread> threads;
        if (categoryId != null) {
            if (limit != null && limit == 1) {
                // Dành cho các ô tóm tắt "Bài viết cuối", lấy tuyệt đối 1 bài mới phản hồi nhất bỏ qua Ghim
                if (canSeeInternal) {
                    threads = threadRepository.findFirstByCategoryIdOrderByLastPostAtDesc(categoryId)
                            .map(java.util.Collections::singletonList)
                            .orElse(java.util.Collections.emptyList());
                } else {
                    org.springframework.data.domain.Pageable p = org.springframework.data.domain.PageRequest.of(0, 1);
                    threads = threadRepository.findFirstPublicByCategoryIdOrderByLastPostAtDesc(categoryId, p);
                }
            } else {
                if (canSeeInternal) {
                    threads = threadRepository.findAllByCategoryIdOrderByPinnedDescLastPostAtDesc(categoryId);
                } else {
                    threads = threadRepository.findAllPublicByCategoryIdOrderByPinnedDescLastPostAtDesc(categoryId);
                }
            }
        } else {
            if (canSeeInternal) {
                threads = threadRepository.findAllByOrderByLastPostAtDesc();
            } else {
                threads = threadRepository.findAllPublicOrderByLastPostAtDesc();
            }
        }
        List<ThreadDTO> dtos = threadMapper.toDTOList(threads);
        enrichThreads(dtos);
        threadListCache.put(cacheKey, dtos);
        return ResponseDTO.success(dtos);
    }

    public ResponseDTO<com.forum.dto.PageResponseDTO<ThreadDTO>> getAllThreadsPaged(
            Long categoryId, String keyword, String sortBy, String sortOrder, int page, int size) {
        
        org.springframework.data.domain.Sort sort = org.springframework.data.domain.Sort.unsorted();
        if (sortBy != null && !sortBy.trim().isEmpty()) {
            org.springframework.data.domain.Sort.Direction direction = 
                "desc".equalsIgnoreCase(sortOrder) ? org.springframework.data.domain.Sort.Direction.DESC : org.springframework.data.domain.Sort.Direction.ASC;
            
            String property = sortBy;
            if ("author.username".equals(sortBy)) {
                property = "author.username";
            } else if ("category.name".equals(sortBy)) {
                property = "category.name";
            }
            sort = org.springframework.data.domain.Sort.by(direction, property);
        } else {
            sort = org.springframework.data.domain.Sort.by(
                org.springframework.data.domain.Sort.Order.desc("pinned"),
                org.springframework.data.domain.Sort.Order.desc("lastPostAt")
            );
        }
        
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(page, size, sort);
        boolean canSeeInternal = isUserAuthorizedForInternalThreads();
        
        String searchPattern = "%";
        if (keyword != null && !keyword.trim().isEmpty()) {
            searchPattern = "%" + keyword.trim().toLowerCase() + "%";
        }
        
        org.springframework.data.domain.Page<Thread> threadPage = threadRepository.searchThreads(
            canSeeInternal, categoryId, searchPattern, pageable);
            
        List<ThreadDTO> dtos = threadMapper.toDTOList(threadPage.getContent());
        enrichThreads(dtos);
        
        com.forum.dto.PageResponseDTO<ThreadDTO> pageResponse = new com.forum.dto.PageResponseDTO<>(
            dtos,
            threadPage.getTotalPages(),
            threadPage.getTotalElements(),
            threadPage.getNumber(),
            threadPage.getSize()
        );
        return ResponseDTO.success(pageResponse);
    }


    public ResponseDTO<List<ThreadDTO>> getLatestThreads() {
        boolean canSeeInternal = isUserAuthorizedForInternalThreads();
        String cacheKey = "latest_" + canSeeInternal;
        List<ThreadDTO> cached = threadListCache.get(cacheKey);
        if (cached != null) {
            return ResponseDTO.success(cached);
        }

        List<Thread> threads;
        if (canSeeInternal) {
            threads = threadRepository.findTop20ByOrderByLastPostAtDesc();
        } else {
            org.springframework.data.domain.Pageable p = org.springframework.data.domain.PageRequest.of(0, 20);
            threads = threadRepository.findAllPublicOrderByLastPostAtDesc(p).getContent();
        }
        List<ThreadDTO> dtos = threadMapper.toDTOList(threads);
        enrichThreads(dtos);
        threadListCache.put(cacheKey, dtos);
        return ResponseDTO.success(dtos);
    }

    public ResponseDTO<ThreadDTO> getThreadById(Long id) {
        threadViewIncrementer.incrementViewCountAsync(id);

        ThreadDTO baseDto = threadCache.get(id);
        if (baseDto == null) {
            Thread thread = threadRepository.findByIdEager(id)
                    .orElseThrow(() -> new RuntimeException("Thread not found"));
            baseDto = threadMapper.toDTO(thread);
            baseDto.setReactionSummary(reactionService.getSummaryForThread(id));
            baseDto.setRecentReactors(reactionService.getRecentReactorsForThread(id));
            threadCache.put(id, baseDto);
        }

        if (com.forum.utils.Constants.THREAD_SCOPE_INTERNAL.equals(baseDto.getScope()) && !isUserAuthorizedForInternalThreads()) {
            throw new RuntimeException("Access denied");
        }

        ThreadDTO dto = new ThreadDTO();
        dto.setId(baseDto.getId());
        dto.setTitle(baseDto.getTitle());
        dto.setContent(baseDto.getContent());
        dto.setCategory(baseDto.getCategory());
        dto.setLabel(baseDto.getLabel());
        dto.setAuthor(baseDto.getAuthor());
        dto.setCreatedAt(baseDto.getCreatedAt());
        dto.setLastPostAt(baseDto.getLastPostAt());
        dto.setViewCount(baseDto.getViewCount());
        dto.setReplyCount(baseDto.getReplyCount());
        dto.setPinned(baseDto.isPinned());
        dto.setActive(baseDto.isActive());
        dto.setPoll(baseDto.getPoll());
        dto.setAttachedImages(baseDto.getAttachedImages());
        dto.setReactionSummary(baseDto.getReactionSummary());
        dto.setRecentReactors(baseDto.getRecentReactors());
        dto.setScope(baseDto.getScope());
        dto.setLocked(baseDto.isLocked());

        dto.setCurrentUserReaction(reactionService.getCurrentUserReactionForThread(id));

        return ResponseDTO.success(dto);
    }

    public ResponseDTO<com.forum.dto.PageResponseDTO<ThreadDTO>> getMyThreadsPaged(String username, int page, int size) {
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(page, size);
        org.springframework.data.domain.Page<Thread> threadPage = threadRepository.findByAuthorUsernameOrderByCreatedAtDesc(username, pageable);
        List<ThreadDTO> dtos = threadMapper.toDTOList(threadPage.getContent());
        enrichThreads(dtos);
        
        // Trả lại content cho DTO của bản thân (vì enrichThreads set content = null)
        // Nhưng ở màn hình list của Profile, ta lại cần content để hiển thị dòng 2!
        // Để ý dòng 259: dto.setContent(null). Do đó ta cần khôi phục content sau khi enrichThreads,
        // hoặc viết hàm riêng. Để đơn giản, ta khôi phục content từ DB.
        for (int i = 0; i < dtos.size(); i++) {
            dtos.get(i).setContent(threadPage.getContent().get(i).getContent());
        }

        return ResponseDTO.success(new com.forum.dto.PageResponseDTO<>(
            dtos,
            threadPage.getTotalPages(),
            threadPage.getTotalElements(),
            threadPage.getNumber(),
            threadPage.getSize()
        ));
    }

    private void enrichThreads(List<ThreadDTO> dtos) {
        if (dtos == null || dtos.isEmpty()) return;
        
        List<Long> threadIds = dtos.stream()
                .map(ThreadDTO::getId)
                .filter(java.util.Objects::nonNull)
                .collect(java.util.stream.Collectors.toList());
                
        if (threadIds.isEmpty()) return;
        
        List<Object[]> latestPosts = postRepository.findLatestPostFieldsForThreadIds(threadIds);
        java.util.Map<Long, Object[]> threadIdToPostMap = latestPosts.stream()
                .collect(java.util.stream.Collectors.toMap(row -> (Long) row[0], row -> row, (p1, p2) -> p1));
                
        for (ThreadDTO dto : dtos) {
            Object[] row = threadIdToPostMap.get(dto.getId());
            if (row != null) {
                dto.setLastPostId((Long) row[1]);
                dto.setLastPostAt((java.time.LocalDateTime) row[2]);
                
                if (row[3] != null) {
                    com.forum.dto.UserDTO userDTO = new com.forum.dto.UserDTO();
                    userDTO.setId((Long) row[3]);
                    userDTO.setUsername((String) row[4]);
                    userDTO.setDisplayName((String) row[5]);
                    userDTO.setEmail((String) row[6]);
                    userDTO.setAvatar((String) row[7]);
                    dto.setLastPostAuthor(userDTO);
                }
            }
            // Strip heavy fields from list responses
            dto.setContent(null);
            dto.setPoll(null);
            dto.setAttachedImages(null);
            if (dto.getCategory() != null) {
                dto.getCategory().setSubCategories(null);
            }
            if (dto.getAuthor() != null) {
                dto.getAuthor().setRoles(null);
            }
        }
    }


    private void enrichThread(ThreadDTO dto) {
        if (dto == null || dto.getId() == null) return;
        
        postRepository.findFirstByThreadIdOrderByCreatedAtDesc(dto.getId()).ifPresent(post -> {
            dto.setLastPostId(post.getId());
            dto.setLastPostAt(post.getCreatedAt());
            
            if (post.getAuthor() != null) {
                com.forum.dto.UserDTO userDTO = new com.forum.dto.UserDTO();
                userDTO.setId(post.getAuthor().getId());
                userDTO.setUsername(post.getAuthor().getUsername());
                userDTO.setDisplayName(post.getAuthor().getDisplayName());
                userDTO.setEmail(post.getAuthor().getEmail());
                userDTO.setAvatar(post.getAuthor().getAvatar());
                dto.setLastPostAuthor(userDTO);
            }
        });

        // Enrich reactions
        dto.setReactionSummary(reactionService.getSummaryForThread(dto.getId()));
        dto.setCurrentUserReaction(reactionService.getCurrentUserReactionForThread(dto.getId()));
        dto.setRecentReactors(reactionService.getRecentReactorsForThread(dto.getId()));
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

    public ResponseDTO<ThreadDTO> createThread(ThreadDTO threadDTO) {
        Thread thread = threadMapper.toEntity(threadDTO);
        if (thread.getScope() == null || thread.getScope().trim().isEmpty()) {
            thread.setScope(com.forum.utils.Constants.THREAD_SCOPE_PUBLIC);
        }
        
        // Lấy username từ SecurityContext
        String username = (String) org.springframework.security.core.context.SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();
        
        userRepository.findByUsername(username).ifPresent(thread::setAuthor);
        
        if (thread.getPoll() != null) {
            thread.getPoll().setThread(thread);
            if (thread.getPoll().getOptions() != null) {
                thread.getPoll().getOptions().forEach(opt -> opt.setPoll(thread.getPoll()));
            }
        }
        
        if (threadDTO.getLabel() != null) {
            com.forum.entity.Label label = new com.forum.entity.Label();
            label.setId(threadDTO.getLabel().getId());
            thread.setLabel(label);
        }
        
        Thread saved = threadRepository.save(thread);
        clearListCache();
        
        try {
            searchDocumentRepository.save(mapThreadToSearchDocument(saved));
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        // Gửi thông báo tag/mention cho bài đăng gốc bất đồng bộ sau khi transaction commit thành công
        try {
            if (saved.getAuthor() != null) {
                Long actorId = saved.getAuthor().getId();
                Long threadId = saved.getId();
                if (org.springframework.transaction.support.TransactionSynchronizationManager.isSynchronizationActive()) {
                    org.springframework.transaction.support.TransactionSynchronizationManager.registerSynchronization(
                        new org.springframework.transaction.support.TransactionSynchronization() {
                            @Override
                            public void afterCommit() {
                                notificationService.processMentionsAsync(actorId, threadId);
                            }
                        }
                    );
                } else {
                    notificationService.processMentionsAsync(actorId, threadId);
                }
            }
        } catch (Exception e) {
            // ignore
        }

        return ResponseDTO.success(threadMapper.toDTO(saved));
    }

    public ResponseDTO<ThreadDTO> updateThread(Long id, ThreadDTO threadDTO) {
        return threadRepository.findById(id).map(thread -> {
            thread.setTitle(threadDTO.getTitle());
            thread.setContent(threadDTO.getContent());
            if (threadDTO.getScope() != null && !threadDTO.getScope().trim().isEmpty()) {
                thread.setScope(threadDTO.getScope());
            }
            if (threadDTO.getCategory() != null) {
                com.forum.entity.Category category = new com.forum.entity.Category();
                category.setId(threadDTO.getCategory().getId());
                thread.setCategory(category);
            }
            
            if (threadDTO.getLabel() != null && threadDTO.getLabel().getId() != null) {
                com.forum.entity.Label label = new com.forum.entity.Label();
                label.setId(threadDTO.getLabel().getId());
                thread.setLabel(label);
            } else {
                thread.setLabel(null);
            }
            
            
            thread.setPinned(threadDTO.isPinned());
            thread.setLocked(threadDTO.isLocked());
            thread.setAttachedImages(threadDTO.getAttachedImages());

            // Xử lý cập nhật Poll
            if (threadDTO.getPoll() != null) {
                com.forum.entity.Poll existingPoll = thread.getPoll();
                if (existingPoll == null) {
                    com.forum.entity.Poll newPoll = threadMapper.toEntity(threadDTO).getPoll();
                    newPoll.setThread(thread);
                    if (newPoll.getOptions() != null) {
                        newPoll.getOptions().forEach(opt -> opt.setPoll(newPoll));
                    }
                    thread.setPoll(newPoll);
                } else {
                    existingPoll.setQuestion(threadDTO.getPoll().getQuestion());
                    existingPoll.setMaxChoices(threadDTO.getPoll().getMaxChoices());
                    existingPoll.setAllowChangeVote(threadDTO.getPoll().isAllowChangeVote());
                    existingPoll.setPublicVoting(threadDTO.getPoll().isPublicVoting());
                    existingPoll.setShowResultWithoutVote(threadDTO.getPoll().isShowResultWithoutVote());
                    existingPoll.setClosedAt(threadDTO.getPoll().getClosedAt());

                    // Cập nhật Options
                    if (threadDTO.getPoll().getOptions() != null) {
                        java.util.List<Long> dtoOptionIds = threadDTO.getPoll().getOptions().stream()
                                .filter(o -> o.getId() != null)
                                .map(com.forum.dto.PollOptionDTO::getId)
                                .collect(java.util.stream.Collectors.toList());

                        // Xóa các option không còn tồn tại trong request mới (và có ID)
                        existingPoll.getOptions().removeIf(opt -> {
                            if (opt.getId() != null && !dtoOptionIds.contains(opt.getId())) {
                                pollVoteRepository.deleteByOptionId(opt.getId());
                                return true;
                            }
                            return false;
                        });

                        // Thêm mới hoặc cập nhật option
                        for (com.forum.dto.PollOptionDTO optDto : threadDTO.getPoll().getOptions()) {
                            if (optDto.getId() != null) {
                                existingPoll.getOptions().stream()
                                        .filter(o -> o.getId().equals(optDto.getId()))
                                        .findFirst()
                                        .ifPresent(o -> o.setOptionText(optDto.getOptionText()));
                            } else {
                                com.forum.entity.PollOption newOpt = new com.forum.entity.PollOption();
                                newOpt.setOptionText(optDto.getOptionText());
                                newOpt.setPoll(existingPoll);
                                existingPoll.getOptions().add(newOpt);
                            }
                        }
                    }
                }
            }

            Thread saved = threadRepository.save(thread);
            evictCache(id);
            clearListCache();
            try {
                searchDocumentRepository.save(mapThreadToSearchDocument(saved));
                List<SearchDocument> posts = searchDocumentRepository.findByThreadId(saved.getId());
                boolean updatedAny = false;
                for (SearchDocument doc : posts) {
                    if ("post".equals(doc.getType())) {
                        doc.setThreadTitle(saved.getTitle());
                        doc.setScope(saved.getScope());
                        doc.setActive(saved.isActive());
                        doc.setCategoryName(saved.getCategory() != null ? saved.getCategory().getName() : null);
                        doc.setCategoryId(saved.getCategory() != null ? saved.getCategory().getId() : null);
                        updatedAny = true;
                    }
                }
                if (updatedAny) {
                    searchDocumentRepository.saveAll(posts);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return ResponseDTO.success(threadMapper.toDTO(saved));
        }).orElseThrow(() -> new RuntimeException("Thread not found"));
    }

    public ResponseDTO<Void> deleteThread(Long id) {
        evictCache(id);
        threadRepository.findById(id).ifPresent(thread -> {
            // 0. Delete thread subscriptions related to this thread
            threadSubscriptionRepository.deleteByThreadId(id);

            // 1. Delete notifications related to this thread and its posts
            notificationRepository.deleteByThreadId(id);
            
            // 2. Handle poll votes if exists
            if (thread.getPoll() != null) {
                pollVoteRepository.deleteByPollId(thread.getPoll().getId());
            }
            
            // 3. Delete all reactions for the thread and its posts
            reactionService.deleteAllReactionsForThread(id);
            
            // 4. Delete posts manually to be safe before thread
            postRepository.deleteByThreadId(id);
            
            // 5. Clear the collections in the managed entity to avoid Hibernate sync issues
            if (thread.getPosts() != null) {
                thread.getPosts().clear();
            }
            
            // 6. Finally delete the thread
            threadRepository.delete(thread);
            clearListCache();

            try {
                searchDocumentRepository.deleteById("thread_" + id);
                searchDocumentRepository.deleteByThreadId(id);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        return ResponseDTO.success(null);
    }

    public ResponseDTO<ThreadDTO> togglePin(Long id) {
        return threadRepository.findById(id).map(thread -> {
            thread.setPinned(!thread.isPinned());
            Thread saved = threadRepository.save(thread);
            evictCache(id);
            clearListCache();
            return ResponseDTO.success(threadMapper.toDTO(saved));
        }).orElseThrow(() -> new RuntimeException("Thread not found"));
    }

    public ResponseDTO<Boolean> getFollowStatus(Long id) {
        Thread thread = threadRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Thread not found"));

        org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getPrincipal() == null || "anonymousUser".equals(auth.getPrincipal())) {
            return ResponseDTO.success(false);
        }

        String username = (String) auth.getPrincipal();
        User currentUser = userRepository.findByUsername(username).orElse(null);
        if (currentUser == null) {
            return ResponseDTO.success(false);
        }

        Optional<ThreadSubscription> subOpt = threadSubscriptionRepository.findByThreadIdAndUserId(id, currentUser.getId());
        if (subOpt.isPresent()) {
            return ResponseDTO.success(subOpt.get().isFollowing());
        } else {
            // Default follow status: True for thread owner, False for others
            boolean isOwner = thread.getAuthor() != null && thread.getAuthor().getId().equals(currentUser.getId());
            return ResponseDTO.success(isOwner);
        }
    }

    public ResponseDTO<Void> setFollowStatus(Long id, boolean following) {
        Thread thread = threadRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Thread not found"));

        String username = (String) org.springframework.security.core.context.SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();
        if ("anonymousUser".equals(username)) {
            throw new RuntimeException("User must be logged in to follow thread");
        }

        User currentUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Optional<ThreadSubscription> subOpt = threadSubscriptionRepository.findByThreadIdAndUserId(id, currentUser.getId());
        if (subOpt.isPresent()) {
            ThreadSubscription sub = subOpt.get();
            sub.setFollowing(following);
            threadSubscriptionRepository.save(sub);
        } else {
            ThreadSubscription sub = new ThreadSubscription();
            sub.setThread(thread);
            sub.setUser(currentUser);
            sub.setFollowing(following);
            threadSubscriptionRepository.save(sub);
        }

        return ResponseDTO.success(null);
    }
}
