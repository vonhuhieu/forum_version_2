package com.forum.service;

import com.forum.dto.PostDTO;
import com.forum.dto.ReactionIconDTO;
import com.forum.dto.ReactionSummaryDTO;
import com.forum.dto.ReceivedReactionDTO;
import com.forum.dto.LabelDTO;
import com.forum.entity.*;
import com.forum.entity.Thread;
import com.forum.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.scheduling.annotation.Async;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
@RequiredArgsConstructor
public class ReactionService {

    private final ReactionRepository reactionRepository;
    private final ReactionIconRepository reactionIconRepository;
    private final ThreadRepository threadRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final ReactionIconService reactionIconService;
    private final NotificationService notificationService;
    private final ConversationMessageRepository conversationMessageRepository;
    private final UserTitleService userTitleService;

    @org.springframework.beans.factory.annotation.Autowired
    @org.springframework.context.annotation.Lazy
    private PostService postService;

    @org.springframework.beans.factory.annotation.Autowired
    @org.springframework.context.annotation.Lazy
    private ThreadService threadService;

    private Optional<User> getCurrentUser() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof String && !principal.equals("anonymousUser")) {
            return userRepository.findByUsername((String) principal);
        }
        return Optional.empty();
    }

    public boolean isUserAuthorizedForInternalThreads() {
        org.springframework.security.core.Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return false;
        }
        return auth.getAuthorities().stream()
                .map(org.springframework.security.core.GrantedAuthority::getAuthority)
                .noneMatch(role -> role.equals(com.forum.utils.Constants.ROLE_NON_OFFICIAL_USER));
    }

    public void reactToThread(Long threadId, Long iconId) {
        User currentUser = getCurrentUser().orElseThrow(() -> new RuntimeException("Authentication required"));
        Thread thread = threadRepository.findById(threadId)
                .orElseThrow(() -> new RuntimeException("Thread not found"));

        if (com.forum.utils.Constants.THREAD_SCOPE_INTERNAL.equals(thread.getScope()) && !isUserAuthorizedForInternalThreads()) {
            throw new RuntimeException("Access denied");
        }

        ReactionIcon icon = reactionIconRepository.findById(iconId)
                .orElseThrow(() -> new RuntimeException("Reaction icon not found"));

        // Check and hide if author reacts to self
        if (thread.getAuthor() != null && thread.getAuthor().getId().equals(currentUser.getId())) {
            throw new RuntimeException("You cannot react to your own content");
        }

        // UPSERT logic: Check for existing reaction by this user on this thread
        Optional<Reaction> existing = reactionRepository.findByUserIdAndThreadId(currentUser.getId(), threadId);
        if (existing.isPresent()) {
            Reaction reaction = existing.get();
            reaction.setReactionIcon(icon);
            reactionRepository.save(reaction);
        } else {
            Reaction newReaction = new Reaction();
            newReaction.setUser(currentUser);
            newReaction.setThread(thread);
            newReaction.setReactionIcon(icon);
            reactionRepository.save(newReaction);

            thread.setReactionCount(thread.getReactionCount() + 1);
            threadRepository.save(thread);
        }

        threadService.evictCache(threadId);

        // Gửi thông báo cho chủ bài viết
        try {
            notificationService.sendReactionNotification(currentUser, thread.getAuthor(), thread, null, icon);
        } catch (Exception e) {
            // Don't block reaction if notification fails
        }
    }

    @Async
    @Transactional
    public void reactToThreadAsync(String username, Long threadId, Long iconId, boolean canSeeInternal) {
        if (username == null || username.equals("anonymousUser")) return;
        User currentUser = userRepository.findByUsername(username).orElse(null);
        if (currentUser == null) return;

        Thread thread = threadRepository.findById(threadId).orElse(null);
        if (thread == null) return;

        if (com.forum.utils.Constants.THREAD_SCOPE_INTERNAL.equals(thread.getScope()) && !canSeeInternal) {
            return;
        }

        ReactionIcon icon = reactionIconRepository.findById(iconId).orElse(null);
        if (icon == null) return;

        if (thread.getAuthor() != null && thread.getAuthor().getId().equals(currentUser.getId())) {
            return;
        }

        Optional<Reaction> existing = reactionRepository.findByUserIdAndThreadId(currentUser.getId(), threadId);
        if (existing.isPresent()) {
            Reaction reaction = existing.get();
            reaction.setReactionIcon(icon);
            reactionRepository.save(reaction);
        } else {
            Reaction newReaction = new Reaction();
            newReaction.setUser(currentUser);
            newReaction.setThread(thread);
            newReaction.setReactionIcon(icon);
            reactionRepository.save(newReaction);

            thread.setReactionCount(thread.getReactionCount() + 1);
            threadRepository.save(thread);
        }

        threadService.evictCache(threadId);

        try {
            notificationService.sendReactionNotification(currentUser, thread.getAuthor(), thread, null, icon);
        } catch (Exception e) {
            // Don't block
        }
    }

    @Async
    @Transactional
    public void removeReactionFromThreadAsync(String username, Long threadId) {
        if (username == null || username.equals("anonymousUser")) return;
        User currentUser = userRepository.findByUsername(username).orElse(null);
        if (currentUser == null) return;

        Optional<Reaction> existing = reactionRepository.findByUserIdAndThreadId(currentUser.getId(), threadId);
        if (existing.isPresent()) {
            reactionRepository.delete(existing.get());
            threadRepository.findById(threadId).ifPresent(thread -> {
                thread.setReactionCount(Math.max(0, thread.getReactionCount() - 1));
                threadRepository.save(thread);
            });
        }
        threadService.evictCache(threadId);
    }

    public void reactToPost(Long postId, Long iconId) {
        User currentUser = getCurrentUser().orElseThrow(() -> new RuntimeException("Authentication required"));
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post not found"));

        if (post.getThread() != null && com.forum.utils.Constants.THREAD_SCOPE_INTERNAL.equals(post.getThread().getScope()) && !isUserAuthorizedForInternalThreads()) {
            throw new RuntimeException("Access denied");
        }

        ReactionIcon icon = reactionIconRepository.findById(iconId)
                .orElseThrow(() -> new RuntimeException("Reaction icon not found"));

        // Check and hide if author reacts to self
        if (post.getAuthor() != null && post.getAuthor().getId().equals(currentUser.getId())) {
            throw new RuntimeException("You cannot react to your own content");
        }

        // UPSERT logic: Check for existing reaction by this user on this post
        Optional<Reaction> existing = reactionRepository.findByUserIdAndPostId(currentUser.getId(), postId);
        if (existing.isPresent()) {
            Reaction reaction = existing.get();
            reaction.setReactionIcon(icon);
            reactionRepository.save(reaction);
        } else {
            Reaction newReaction = new Reaction();
            newReaction.setUser(currentUser);
            newReaction.setPost(post);
            newReaction.setReactionIcon(icon);
            reactionRepository.save(newReaction);
        }

        if (post.getThread() != null) {
            postService.evictCache(post.getThread().getId());
        }

        // Gửi thông báo cho chủ bình luận
        try {
            notificationService.sendReactionNotification(currentUser, post.getAuthor(), post.getThread(), post, icon);
        } catch (Exception e) {
            // Don't block
        }
    }

    @Async
    @Transactional
    public void reactToPostAsync(String username, Long postId, Long iconId, boolean canSeeInternal) {
        if (username == null || username.equals("anonymousUser")) return;
        User currentUser = userRepository.findByUsername(username).orElse(null);
        if (currentUser == null) return;

        Post post = postRepository.findById(postId).orElse(null);
        if (post == null) return;

        if (post.getThread() != null && com.forum.utils.Constants.THREAD_SCOPE_INTERNAL.equals(post.getThread().getScope()) && !canSeeInternal) {
            return;
        }

        ReactionIcon icon = reactionIconRepository.findById(iconId).orElse(null);
        if (icon == null) return;

        if (post.getAuthor() != null && post.getAuthor().getId().equals(currentUser.getId())) {
            return;
        }

        Optional<Reaction> existing = reactionRepository.findByUserIdAndPostId(currentUser.getId(), postId);
        if (existing.isPresent()) {
            Reaction reaction = existing.get();
            reaction.setReactionIcon(icon);
            reactionRepository.save(reaction);
        } else {
            Reaction newReaction = new Reaction();
            newReaction.setUser(currentUser);
            newReaction.setPost(post);
            newReaction.setReactionIcon(icon);
            reactionRepository.save(newReaction);
        }

        if (post.getThread() != null) {
            postService.evictCache(post.getThread().getId());
        }

        try {
            notificationService.sendReactionNotification(currentUser, post.getAuthor(), post.getThread(), post, icon);
        } catch (Exception e) {
            // Don't block
        }
    }

    @Async
    @Transactional
    public void removeReactionFromPostAsync(String username, Long postId) {
        if (username == null || username.equals("anonymousUser")) return;
        User currentUser = userRepository.findByUsername(username).orElse(null);
        if (currentUser == null) return;

        postRepository.findById(postId).ifPresent(post -> {
            if (post.getThread() != null) {
                postService.evictCache(post.getThread().getId());
            }
        });
        reactionRepository.deleteByUserIdAndPostId(currentUser.getId(), postId);
    }

    public void removeReactionFromThread(Long threadId) {
        User currentUser = getCurrentUser().orElseThrow(() -> new RuntimeException("Authentication required"));
        Optional<Reaction> existing = reactionRepository.findByUserIdAndThreadId(currentUser.getId(), threadId);
        if (existing.isPresent()) {
            reactionRepository.delete(existing.get());
            threadRepository.findById(threadId).ifPresent(thread -> {
                thread.setReactionCount(Math.max(0, thread.getReactionCount() - 1));
                threadRepository.save(thread);
            });
        }
        threadService.evictCache(threadId);
    }

    public void removeReactionFromPost(Long postId) {
        User currentUser = getCurrentUser().orElseThrow(() -> new RuntimeException("Authentication required"));
        postRepository.findById(postId).ifPresent(post -> {
            if (post.getThread() != null) {
                postService.evictCache(post.getThread().getId());
            }
        });
        reactionRepository.deleteByUserIdAndPostId(currentUser.getId(), postId);
    }

    public List<ReactionSummaryDTO> getSummaryForThread(Long threadId) {
        List<Object[]> results = reactionRepository.aggregateByThreadId(threadId);
        return mapAggregateResults(results);
    }

    public List<ReactionSummaryDTO> getSummaryForPost(Long postId) {
        List<Object[]> results = reactionRepository.aggregateByPostId(postId);
        return mapAggregateResults(results);
    }

    public ReactionIconDTO getCurrentUserReactionForThread(Long threadId) {
        return getCurrentUser().flatMap(user -> 
            reactionRepository.findByUserIdAndThreadId(user.getId(), threadId)
                    .map(Reaction::getReactionIcon)
                    .map(reactionIconService::convertToDTO)
        ).orElse(null);
    }

    public ReactionIconDTO getCurrentUserReactionForPost(Long postId) {
        return getCurrentUser().flatMap(user -> 
            reactionRepository.findByUserIdAndPostId(user.getId(), postId)
                    .map(Reaction::getReactionIcon)
                    .map(reactionIconService::convertToDTO)
        ).orElse(null);
    }

    private List<ReactionSummaryDTO> mapAggregateResults(List<Object[]> results) {
        List<ReactionSummaryDTO> summaries = new ArrayList<>();
        if (results == null) return summaries;
        
        for (Object[] row : results) {
            if (row != null && row.length == 3) {
                ReactionIcon icon = (ReactionIcon) row[0];
                Long count = (Long) row[1];
                java.time.LocalDateTime latestTime = (java.time.LocalDateTime) row[2];
                summaries.add(new ReactionSummaryDTO(reactionIconService.convertToDTO(icon), count, latestTime));
            }
        }
        return summaries;
    }

    public void enrichPostsWithReactions(List<PostDTO> dtos, Long threadId) {
        if (dtos == null || dtos.isEmpty()) return;

        List<Reaction> allReactions = reactionRepository.findAllByPostThreadId(threadId);

        java.util.Map<Long, List<Reaction>> reactionsByPostId = allReactions.stream()
                .filter(r -> r.getPost() != null && r.getPost().getId() != null)
                .collect(java.util.stream.Collectors.groupingBy(r -> r.getPost().getId()));

        User currentUser = getCurrentUser().orElse(null);

        for (PostDTO dto : dtos) {
            List<Reaction> postReactions = reactionsByPostId.get(dto.getId());

            if (postReactions == null || postReactions.isEmpty()) {
                dto.setReactionSummary(new ArrayList<>());
                dto.setCurrentUserReaction(null);
                dto.setRecentReactors(new ArrayList<>());
                continue;
            }

            java.util.Map<ReactionIcon, List<Reaction>> groupedByIcon = postReactions.stream()
                    .filter(r -> r.getReactionIcon() != null)
                    .collect(java.util.stream.Collectors.groupingBy(Reaction::getReactionIcon));

            List<ReactionSummaryDTO> summary = new ArrayList<>();
            for (java.util.Map.Entry<ReactionIcon, List<Reaction>> entry : groupedByIcon.entrySet()) {
                ReactionIcon icon = entry.getKey();
                List<Reaction> iconReactions = entry.getValue();
                long count = iconReactions.size();
                java.time.LocalDateTime latestTime = iconReactions.stream()
                        .map(r -> r.getUpdatedAt() != null ? r.getUpdatedAt() : r.getCreatedAt())
                        .filter(java.util.Objects::nonNull)
                        .max(java.time.LocalDateTime::compareTo)
                        .orElse(null);
                summary.add(new ReactionSummaryDTO(reactionIconService.convertToDTO(icon), count, latestTime));
            }
            summary.sort((s1, s2) -> Long.compare(s2.getCount(), s1.getCount()));
            dto.setReactionSummary(summary);

            ReactionIconDTO userReaction = null;
            if (currentUser != null) {
                userReaction = postReactions.stream()
                        .filter(r -> r.getUser() != null && r.getUser().getId().equals(currentUser.getId()))
                        .findFirst()
                        .map(Reaction::getReactionIcon)
                        .map(reactionIconService::convertToDTO)
                        .orElse(null);
            }
            dto.setCurrentUserReaction(userReaction);

            List<com.forum.dto.UserDTO> recentReactors = postReactions.stream()
                    .sorted((r1, r2) -> {
                        java.time.LocalDateTime t1 = r1.getUpdatedAt() != null ? r1.getUpdatedAt() : r1.getCreatedAt();
                        java.time.LocalDateTime t2 = r2.getUpdatedAt() != null ? r2.getUpdatedAt() : r2.getCreatedAt();
                        if (t1 == null) return 1;
                        if (t2 == null) return -1;
                        return t2.compareTo(t1);
                    })
                    .limit(3)
                    .map(Reaction::getUser)
                    .filter(java.util.Objects::nonNull)
                    .map(this::mapUserToDTO)
                    .collect(java.util.stream.Collectors.toList());
            dto.setRecentReactors(recentReactors);
        }
    }

    private com.forum.dto.UserDTO mapUserToDTO(User user) {
        if (user == null) return null;
        com.forum.dto.UserDTO dto = new com.forum.dto.UserDTO();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setDisplayName(user.getDisplayName());
        dto.setEmail(user.getEmail());
        dto.setAvatar(user.getAvatar());

        long threadCount = threadRepository.countByAuthorId(user.getId());
        long postCountInDb = postRepository.countByAuthorId(user.getId());
        long totalPosts = threadCount + postCountInDb;
        long interactionPoints = reactionRepository.countReactionsReceivedByUserId(user.getId());
        long trophyPoints = Math.round(totalPosts * 0.1 + interactionPoints * 0.2);

        dto.setPostCount(totalPosts);
        dto.setInteractionPoints(interactionPoints);
        dto.setTrophyPoints(trophyPoints);
        dto.setThreadCount(threadCount);
        dto.setCommentCount(postCountInDb);
        dto.setDisplayTitle(userTitleService.resolveDisplayTitle(user, trophyPoints));
        dto.setIsVerifiedBadge(userTitleService.isVerifiedBadge(user, trophyPoints));
        return dto;
    }

    public List<com.forum.dto.UserDTO> getRecentReactorsForThread(Long threadId) {
        List<Reaction> reactions = reactionRepository.findTop3ByThreadIdOrderByUpdatedAtDesc(threadId);
        return reactions.stream()
                .map(Reaction::getUser)
                .map(this::mapUserToDTO)
                .collect(java.util.stream.Collectors.toList());
    }

    public List<com.forum.dto.UserDTO> getRecentReactorsForPost(Long postId) {
        List<Reaction> reactions = reactionRepository.findTop3ByPostIdOrderByUpdatedAtDesc(postId);
        return reactions.stream()
                .map(Reaction::getUser)
                .map(this::mapUserToDTO)
                .collect(java.util.stream.Collectors.toList());
    }

    public void deleteAllReactionsForThread(Long threadId) {
        reactionRepository.deleteByThreadIdOrPostThreadId(threadId);
    }

    private com.forum.dto.ReactionParticipantDTO mapToParticipantDTO(Reaction reaction) {
        return new com.forum.dto.ReactionParticipantDTO(
                mapUserToDTO(reaction.getUser()),
                reactionIconService.convertToDTO(reaction.getReactionIcon()),
                reaction.getUpdatedAt() != null ? reaction.getUpdatedAt() : reaction.getCreatedAt()
        );
    }

    public org.springframework.data.domain.Page<com.forum.dto.ReactionParticipantDTO> getThreadReactionParticipants(Long threadId, Long iconId, org.springframework.data.domain.Pageable pageable) {
        org.springframework.data.domain.Page<Reaction> reactions;
        if (iconId != null) {
            reactions = reactionRepository.findByThreadIdAndReactionIconId(threadId, iconId, pageable);
        } else {
            reactions = reactionRepository.findByThreadId(threadId, pageable);
        }
        return reactions.map(this::mapToParticipantDTO);
    }

    public org.springframework.data.domain.Page<com.forum.dto.ReactionParticipantDTO> getPostReactionParticipants(Long postId, Long iconId, org.springframework.data.domain.Pageable pageable) {
        org.springframework.data.domain.Page<Reaction> reactions;
        if (iconId != null) {
            reactions = reactionRepository.findByPostIdAndReactionIconId(postId, iconId, pageable);
        } else {
            reactions = reactionRepository.findByPostId(postId, pageable);
        }
        return reactions.map(this::mapToParticipantDTO);
    }

    public void reactToMessage(Long messageId, Long iconId) {
        User currentUser = getCurrentUser().orElseThrow(() -> new RuntimeException("Authentication required"));
        ConversationMessage message = conversationMessageRepository.findById(messageId)
                .orElseThrow(() -> new RuntimeException("Message not found"));
        ReactionIcon icon = reactionIconRepository.findById(iconId)
                .orElseThrow(() -> new RuntimeException("Reaction icon not found"));

        if (message.getSender() != null && message.getSender().getId().equals(currentUser.getId())) {
            throw new RuntimeException("You cannot react to your own content");
        }

        Optional<Reaction> existing = reactionRepository.findByUserIdAndConversationMessageId(currentUser.getId(), messageId);
        if (existing.isPresent()) {
            Reaction reaction = existing.get();
            reaction.setReactionIcon(icon);
            reactionRepository.save(reaction);
        } else {
            Reaction newReaction = new Reaction();
            newReaction.setUser(currentUser);
            newReaction.setConversationMessage(message);
            newReaction.setReactionIcon(icon);
            reactionRepository.save(newReaction);
        }

        try {
            notificationService.sendConversationReactionNotification(currentUser.getId(), message.getSender().getId(), message.getConversation().getId(), message.getId(), icon.getId());
        } catch (Exception e) {
            // Don't block reaction
        }
    }

    public void removeReactionFromMessage(Long messageId) {
        User currentUser = getCurrentUser().orElseThrow(() -> new RuntimeException("Authentication required"));
        reactionRepository.deleteByUserIdAndConversationMessageId(currentUser.getId(), messageId);
    }

    @Async
    @Transactional
    public void reactToMessageAsync(String username, Long messageId, Long iconId) {
        if (username == null || username.equals("anonymousUser")) return;
        User currentUser = userRepository.findByUsername(username).orElse(null);
        if (currentUser == null) return;

        ConversationMessage message = conversationMessageRepository.findById(messageId).orElse(null);
        if (message == null) return;

        ReactionIcon icon = reactionIconRepository.findById(iconId).orElse(null);
        if (icon == null) return;

        if (message.getSender() != null && message.getSender().getId().equals(currentUser.getId())) {
            return;
        }

        Optional<Reaction> existing = reactionRepository.findByUserIdAndConversationMessageId(currentUser.getId(), messageId);
        if (existing.isPresent()) {
            Reaction reaction = existing.get();
            reaction.setReactionIcon(icon);
            reactionRepository.save(reaction);
        } else {
            Reaction newReaction = new Reaction();
            newReaction.setUser(currentUser);
            newReaction.setConversationMessage(message);
            newReaction.setReactionIcon(icon);
            reactionRepository.save(newReaction);
        }

        try {
            notificationService.sendConversationReactionNotification(currentUser.getId(), message.getSender().getId(), message.getConversation().getId(), message.getId(), icon.getId());
        } catch (Exception e) {
            // Don't block
        }
    }

    @Async
    @Transactional
    public void removeReactionFromMessageAsync(String username, Long messageId) {
        if (username == null || username.equals("anonymousUser")) return;
        User currentUser = userRepository.findByUsername(username).orElse(null);
        if (currentUser == null) return;

        reactionRepository.deleteByUserIdAndConversationMessageId(currentUser.getId(), messageId);
    }

    public List<ReactionSummaryDTO> getSummaryForMessage(Long messageId) {
        List<Object[]> results = reactionRepository.aggregateByConversationMessageId(messageId);
        return mapAggregateResults(results);
    }

    public ReactionIconDTO getCurrentUserReactionForMessage(Long messageId) {
        return getCurrentUser().flatMap(user -> 
            reactionRepository.findByUserIdAndConversationMessageId(user.getId(), messageId)
                    .map(Reaction::getReactionIcon)
                    .map(reactionIconService::convertToDTO)
        ).orElse(null);
    }

    public List<com.forum.dto.UserDTO> getRecentReactorsForMessage(Long messageId) {
        List<Reaction> reactions = reactionRepository.findTop3ByConversationMessageIdOrderByUpdatedAtDesc(messageId);
        return reactions.stream()
                .map(Reaction::getUser)
                .map(this::mapUserToDTO)
                .collect(java.util.stream.Collectors.toList());
    }

    public org.springframework.data.domain.Page<com.forum.dto.ReactionParticipantDTO> getMessageReactionParticipants(Long messageId, Long iconId, org.springframework.data.domain.Pageable pageable) {
        org.springframework.data.domain.Page<Reaction> reactions;
        if (iconId != null) {
            reactions = reactionRepository.findByConversationMessageIdAndReactionIconId(messageId, iconId, pageable);
        } else {
            reactions = reactionRepository.findByConversationMessageId(messageId, pageable);
        }
        return reactions.map(this::mapToParticipantDTO);
    }

    public List<Reaction> getUserReactionsInThread(String username, Long threadId) {
        return reactionRepository.findAllByUsernameAndThreadId(username, threadId);
    }

    public ReactionIconDTO convertToIconDTO(ReactionIcon icon) {
        return reactionIconService.convertToDTO(icon);
    }

    public List<ReactionSummaryDTO> getReceivedReactionsSummary() {
        User currentUser = getCurrentUser().orElseThrow(() -> new RuntimeException("Authentication required"));
        List<Object[]> results = reactionRepository.aggregateReactionsReceivedByUserId(currentUser.getId());
        return mapAggregateResults(results);
    }

    public org.springframework.data.domain.Page<ReceivedReactionDTO> getReceivedReactions(Long iconId, org.springframework.data.domain.Pageable pageable) {
        User currentUser = getCurrentUser().orElseThrow(() -> new RuntimeException("Authentication required"));
        org.springframework.data.domain.Page<Reaction> reactions = reactionRepository.findReactionsReceivedByUserId(currentUser.getId(), iconId, pageable);
        return reactions.map(this::convertToReceivedReactionDTO);
    }

    private ReceivedReactionDTO convertToReceivedReactionDTO(Reaction r) {
        ReceivedReactionDTO dto = new ReceivedReactionDTO();
        dto.setId(r.getId());
        dto.setActor(mapUserToDTO(r.getUser()));
        dto.setReactionIcon(reactionIconService.convertToDTO(r.getReactionIcon()));
        dto.setInteractedAt(r.getUpdatedAt() != null ? r.getUpdatedAt() : r.getCreatedAt());

        if (r.getPost() != null) {
            Post post = r.getPost();
            dto.setPostId(post.getId());
            dto.setContent(post.getContent());
            dto.setTargetCreatedAt(post.getCreatedAt());
            if (post.getThread() != null) {
                Thread thread = post.getThread();
                dto.setThreadId(thread.getId());
                dto.setThreadTitle(thread.getTitle());
                if (thread.getLabel() != null) {
                    dto.setThreadLabel(new LabelDTO(
                        thread.getLabel().getId(),
                        thread.getLabel().getName(),
                        thread.getLabel().getColorCode(),
                        thread.getLabel().getTextColor(),
                        thread.getLabel().getBorderColor(),
                        thread.getLabel().getAdminOnly()
                    ));
                }
            }
        } else if (r.getThread() != null) {
            Thread thread = r.getThread();
            dto.setThreadId(thread.getId());
            dto.setThreadTitle(thread.getTitle());
            dto.setContent(thread.getContent());
            dto.setTargetCreatedAt(thread.getCreatedAt());
            if (thread.getLabel() != null) {
                dto.setThreadLabel(new LabelDTO(
                    thread.getLabel().getId(),
                    thread.getLabel().getName(),
                    thread.getLabel().getColorCode(),
                    thread.getLabel().getTextColor(),
                    thread.getLabel().getBorderColor(),
                    thread.getLabel().getAdminOnly()
                ));
            }
        }
        return dto;
    }
}
