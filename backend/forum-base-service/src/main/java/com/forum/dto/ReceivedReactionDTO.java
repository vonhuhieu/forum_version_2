package com.forum.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ReceivedReactionDTO {
    private Long id;
    private UserDTO actor;
    private ReactionIconDTO reactionIcon;
    private Long threadId;
    private String threadTitle;
    private LabelDTO threadLabel;
    private Long postId;
    private String content;
    private LocalDateTime targetCreatedAt;
    private LocalDateTime interactedAt;
}
