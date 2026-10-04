package com.forum.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProfilePostDTO {
    private Long id;
    private String content;
    private UserDTO author;
    private Long profileUserId;
    private String profileUsername;
    private int commentsCount;
    private long totalComments;
    private List<ProfilePostCommentDTO> latestComments;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private boolean canEdit;
    private boolean canDelete;

    private List<ReactionSummaryDTO> reactionSummary;
    private ReactionIconDTO currentUserReaction;
}
