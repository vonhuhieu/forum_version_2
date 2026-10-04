package com.forum.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class ConversationMessageDTO {
    private Long id;
    private String content;
    private UserDTO sender;
    private LocalDateTime createdAt;
    
    private ReactionIconDTO currentUserReaction;
    private List<ReactionSummaryDTO> reactionSummary;
    private List<UserDTO> recentReactors;
}
