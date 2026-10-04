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
public class ProfilePostCommentDTO {
    private Long id;
    private Long profilePostId;
    private String content;
    private UserDTO author;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private boolean canEdit;
    private boolean canDelete;

    private List<ReactionSummaryDTO> reactionSummary;
    private ReactionIconDTO currentUserReaction;
}
