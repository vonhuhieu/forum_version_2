package com.forum.service;

import com.forum.dto.*;
import com.forum.entity.ProfilePost;
import com.forum.entity.ProfilePostComment;
import com.forum.entity.User;
import com.forum.repository.ProfilePostCommentRepository;
import com.forum.repository.ProfilePostRepository;
import com.forum.repository.ReactionRepository;
import com.forum.repository.UserRepository;
import com.forum.utils.Constants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class ProfilePostService {

    private final ProfilePostRepository profilePostRepository;
    private final ProfilePostCommentRepository profilePostCommentRepository;
    private final UserRepository userRepository;
    private final UserService userService;
    private final ReactionService reactionService;
    private final ReactionRepository reactionRepository;

    private boolean isUserAdmin(User user) {
        if (user == null || user.getRoles() == null) return false;
        return user.getRoles().contains(Constants.ROLE_ADMIN) || user.getRoles().contains(Constants.ROLE_SUPER_ADMIN);
    }

    @Transactional(readOnly = true)
    public PageResponseDTO<ProfilePostDTO> getProfilePostsByUsername(String username, int page, int size, String currentUsername) {
        User profileUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng: " + username));

        User currentUser = (currentUsername != null && !currentUsername.equals("anonymousUser"))
                ? userRepository.findByUsername(currentUsername).orElse(null)
                : null;

        boolean isAdmin = isUserAdmin(currentUser);

        Pageable pageable = PageRequest.of(page, size);
        Page<ProfilePost> postPage = profilePostRepository.findByProfileUserIdOrderByCreatedAtDesc(profileUser.getId(), pageable);

        List<ProfilePostDTO> dtos = postPage.getContent().stream().map(post -> {
            boolean canEdit = currentUser != null && (post.getAuthor().getId().equals(currentUser.getId()) || isAdmin);
            boolean canDelete = currentUser != null && (post.getAuthor().getId().equals(currentUser.getId()) 
                    || post.getProfileUser().getId().equals(currentUser.getId()) 
                    || isAdmin);

            // Lấy 3 bình luận mới nhất (DB sort DESC, reverse để hiển thị tăng dần theo thời gian)
            List<ProfilePostComment> latestCommentsRaw = profilePostCommentRepository.findTop3ByProfilePostIdOrderByCreatedAtDescIdDesc(post.getId());
            List<ProfilePostComment> latestCommentsChronological = new ArrayList<>(latestCommentsRaw);
            Collections.reverse(latestCommentsChronological);

            List<ProfilePostCommentDTO> commentDTOs = latestCommentsChronological.stream().map(c -> {
                boolean cCanEdit = currentUser != null && (c.getAuthor().getId().equals(currentUser.getId()) || isAdmin);
                boolean cCanDelete = currentUser != null && (c.getAuthor().getId().equals(currentUser.getId()) 
                        || post.getProfileUser().getId().equals(currentUser.getId()) 
                        || isAdmin);

                return ProfilePostCommentDTO.builder()
                        .id(c.getId())
                        .profilePostId(post.getId())
                        .content(c.getContent())
                        .author(userService.convertToDTO(c.getAuthor()))
                        .createdAt(c.getCreatedAt())
                        .updatedAt(c.getUpdatedAt())
                        .canEdit(cCanEdit)
                        .canDelete(cCanDelete)
                        .reactionSummary(reactionService.getSummaryForProfilePostComment(c.getId()))
                        .currentUserReaction(reactionService.getCurrentUserReactionForProfilePostComment(c.getId(), currentUsername))
                        .build();
            }).collect(Collectors.toList());

            return ProfilePostDTO.builder()
                    .id(post.getId())
                    .content(post.getContent())
                    .author(userService.convertToDTO(post.getAuthor()))
                    .profileUserId(post.getProfileUser().getId())
                    .profileUsername(post.getProfileUser().getUsername())
                    .commentsCount(post.getCommentsCount())
                    .totalComments(post.getCommentsCount())
                    .latestComments(commentDTOs)
                    .createdAt(post.getCreatedAt())
                    .updatedAt(post.getUpdatedAt())
                    .canEdit(canEdit)
                    .canDelete(canDelete)
                    .reactionSummary(reactionService.getSummaryForProfilePost(post.getId()))
                    .currentUserReaction(reactionService.getCurrentUserReactionForProfilePost(post.getId(), currentUsername))
                    .build();
        }).collect(Collectors.toList());

        return new PageResponseDTO<>(
                dtos,
                postPage.getTotalPages(),
                postPage.getTotalElements(),
                postPage.getNumber(),
                postPage.getSize()
        );
    }

    @Transactional(readOnly = true)
    public PageResponseDTO<ProfilePostCommentDTO> getCommentsByProfilePostId(Long profilePostId, int page, int size, String currentUsername) {
        ProfilePost post = profilePostRepository.findById(profilePostId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bài viết hồ sơ: " + profilePostId));

        User currentUser = (currentUsername != null && !currentUsername.equals("anonymousUser"))
                ? userRepository.findByUsername(currentUsername).orElse(null)
                : null;

        boolean isAdmin = isUserAdmin(currentUser);

        Pageable pageable = PageRequest.of(page, size);
        Page<ProfilePostComment> commentPage = profilePostCommentRepository.findByProfilePostIdOrderByCreatedAtDesc(profilePostId, pageable);

        List<ProfilePostComment> chronologicalList = new ArrayList<>(commentPage.getContent());
        Collections.reverse(chronologicalList);

        List<ProfilePostCommentDTO> dtos = chronologicalList.stream().map(c -> {
            boolean canEdit = currentUser != null && (c.getAuthor().getId().equals(currentUser.getId()) || isAdmin);
            boolean canDelete = currentUser != null && (c.getAuthor().getId().equals(currentUser.getId()) 
                    || post.getProfileUser().getId().equals(currentUser.getId()) 
                    || isAdmin);

            return ProfilePostCommentDTO.builder()
                    .id(c.getId())
                    .profilePostId(post.getId())
                    .content(c.getContent())
                    .author(userService.convertToDTO(c.getAuthor()))
                    .createdAt(c.getCreatedAt())
                    .updatedAt(c.getUpdatedAt())
                    .canEdit(canEdit)
                    .canDelete(canDelete)
                    .reactionSummary(reactionService.getSummaryForProfilePostComment(c.getId()))
                    .currentUserReaction(reactionService.getCurrentUserReactionForProfilePostComment(c.getId(), currentUsername))
                    .build();
        }).collect(Collectors.toList());

        return new PageResponseDTO<>(
                dtos,
                commentPage.getTotalPages(),
                commentPage.getTotalElements(),
                commentPage.getNumber(),
                commentPage.getSize()
        );
    }

    public ProfilePostDTO createProfilePost(ProfilePostCreateDTO dto, String authorUsername) {
        if (dto.getContent() == null || dto.getContent().trim().isEmpty()) {
            throw new RuntimeException("Nội dung bài viết không được để trống");
        }

        User author = userRepository.findByUsername(authorUsername)
                .orElseThrow(() -> new RuntimeException("Author not found: " + authorUsername));

        User profileUser = userRepository.findByUsername(dto.getProfileUsername())
                .orElseThrow(() -> new RuntimeException("Profile user not found: " + dto.getProfileUsername()));

        ProfilePost post = new ProfilePost();
        post.setContent(dto.getContent().trim());
        post.setAuthor(author);
        post.setProfileUser(profileUser);
        post.setCommentsCount(0);

        ProfilePost saved = profilePostRepository.save(post);

        return ProfilePostDTO.builder()
                .id(saved.getId())
                .content(saved.getContent())
                .author(userService.convertToDTO(saved.getAuthor()))
                .profileUserId(saved.getProfileUser().getId())
                .profileUsername(saved.getProfileUser().getUsername())
                .commentsCount(0)
                .totalComments(0)
                .latestComments(new ArrayList<>())
                .createdAt(saved.getCreatedAt())
                .updatedAt(saved.getUpdatedAt())
                .canEdit(true)
                .canDelete(true)
                .reactionSummary(new ArrayList<>())
                .currentUserReaction(null)
                .build();
    }

    public ProfilePostDTO updateProfilePost(Long id, String content, String currentUsername) {
        if (content == null || content.trim().isEmpty()) {
            throw new RuntimeException("Nội dung bài viết không được để trống");
        }

        ProfilePost post = profilePostRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Bài viết không tồn tại"));

        User currentUser = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new RuntimeException("User not found: " + currentUsername));

        boolean isAdmin = isUserAdmin(currentUser);
        if (!post.getAuthor().getId().equals(currentUser.getId()) && !isAdmin) {
            throw new RuntimeException("Bạn không có quyền chỉnh sửa bài viết này");
        }

        post.setContent(content.trim());
        ProfilePost updated = profilePostRepository.save(post);

        return ProfilePostDTO.builder()
                .id(updated.getId())
                .content(updated.getContent())
                .author(userService.convertToDTO(updated.getAuthor()))
                .profileUserId(updated.getProfileUser().getId())
                .profileUsername(updated.getProfileUser().getUsername())
                .commentsCount(updated.getCommentsCount())
                .totalComments(updated.getCommentsCount())
                .createdAt(updated.getCreatedAt())
                .updatedAt(updated.getUpdatedAt())
                .canEdit(true)
                .canDelete(true)
                .reactionSummary(reactionService.getSummaryForProfilePost(updated.getId()))
                .currentUserReaction(reactionService.getCurrentUserReactionForProfilePost(updated.getId(), currentUsername))
                .build();
    }

    public void deleteProfilePost(Long id, String currentUsername) {
        ProfilePost post = profilePostRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Bài viết không tồn tại"));

        User currentUser = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new RuntimeException("User not found: " + currentUsername));

        boolean isAdmin = isUserAdmin(currentUser);
        boolean isAuthor = post.getAuthor().getId().equals(currentUser.getId());
        boolean isProfileOwner = post.getProfileUser().getId().equals(currentUser.getId());

        if (!isAuthor && !isProfileOwner && !isAdmin) {
            throw new RuntimeException("Bạn không có quyền xóa bài viết này");
        }

        // Xóa reactions trên các comments
        List<ProfilePostComment> comments = profilePostCommentRepository.findByProfilePostIdOrderByCreatedAtDesc(id, Pageable.unpaged()).getContent();
        for (ProfilePostComment c : comments) {
            reactionRepository.deleteByProfilePostCommentId(c.getId());
        }

        // Xóa comments
        profilePostCommentRepository.deleteByProfilePostId(id);

        // Xóa reactions trên post
        reactionRepository.deleteByProfilePostId(id);

        // Xóa post
        profilePostRepository.delete(post);
    }

    public ProfilePostCommentDTO createComment(Long profilePostId, ProfilePostCommentCreateDTO dto, String authorUsername) {
        if (dto.getContent() == null || dto.getContent().trim().isEmpty()) {
            throw new RuntimeException("Nội dung bình luận không được để trống");
        }

        ProfilePost post = profilePostRepository.findById(profilePostId)
                .orElseThrow(() -> new RuntimeException("Bài viết không tồn tại"));

        User author = userRepository.findByUsername(authorUsername)
                .orElseThrow(() -> new RuntimeException("User not found: " + authorUsername));

        ProfilePostComment comment = new ProfilePostComment();
        comment.setContent(dto.getContent().trim());
        comment.setProfilePost(post);
        comment.setAuthor(author);

        ProfilePostComment saved = profilePostCommentRepository.save(comment);

        post.setCommentsCount(post.getCommentsCount() + 1);
        profilePostRepository.save(post);

        return ProfilePostCommentDTO.builder()
                .id(saved.getId())
                .profilePostId(post.getId())
                .content(saved.getContent())
                .author(userService.convertToDTO(saved.getAuthor()))
                .createdAt(saved.getCreatedAt())
                .updatedAt(saved.getUpdatedAt())
                .canEdit(true)
                .canDelete(true)
                .reactionSummary(new ArrayList<>())
                .currentUserReaction(null)
                .build();
    }

    public ProfilePostCommentDTO updateComment(Long commentId, String content, String currentUsername) {
        if (content == null || content.trim().isEmpty()) {
            throw new RuntimeException("Nội dung bình luận không được để trống");
        }

        ProfilePostComment comment = profilePostCommentRepository.findById(commentId)
                .orElseThrow(() -> new RuntimeException("Bình luận không tồn tại"));

        User currentUser = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new RuntimeException("User not found: " + currentUsername));

        boolean isAdmin = isUserAdmin(currentUser);
        if (!comment.getAuthor().getId().equals(currentUser.getId()) && !isAdmin) {
            throw new RuntimeException("Bạn không có quyền chỉnh sửa bình luận này");
        }

        comment.setContent(content.trim());
        ProfilePostComment updated = profilePostCommentRepository.save(comment);

        return ProfilePostCommentDTO.builder()
                .id(updated.getId())
                .profilePostId(updated.getProfilePost().getId())
                .content(updated.getContent())
                .author(userService.convertToDTO(updated.getAuthor()))
                .createdAt(updated.getCreatedAt())
                .updatedAt(updated.getUpdatedAt())
                .canEdit(true)
                .canDelete(true)
                .reactionSummary(reactionService.getSummaryForProfilePostComment(updated.getId()))
                .currentUserReaction(reactionService.getCurrentUserReactionForProfilePostComment(updated.getId(), currentUsername))
                .build();
    }

    public void deleteComment(Long commentId, String currentUsername) {
        ProfilePostComment comment = profilePostCommentRepository.findById(commentId)
                .orElseThrow(() -> new RuntimeException("Bình luận không tồn tại"));

        User currentUser = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new RuntimeException("User not found: " + currentUsername));

        ProfilePost post = comment.getProfilePost();
        boolean isAdmin = isUserAdmin(currentUser);
        boolean isAuthor = comment.getAuthor().getId().equals(currentUser.getId());
        boolean isProfileOwner = post.getProfileUser().getId().equals(currentUser.getId());

        if (!isAuthor && !isProfileOwner && !isAdmin) {
            throw new RuntimeException("Bạn không có quyền xóa bình luận này");
        }

        reactionRepository.deleteByProfilePostCommentId(commentId);
        profilePostCommentRepository.delete(comment);

        post.setCommentsCount(Math.max(0, post.getCommentsCount() - 1));
        profilePostRepository.save(post);
    }

    @Transactional(readOnly = true)
    public long getProfilePostCount(Long userId) {
        return profilePostRepository.countByProfileUserId(userId);
    }
}
