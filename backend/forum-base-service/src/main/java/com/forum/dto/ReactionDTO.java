package com.forum.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ReactionDTO {
    private Long id;
    private UserDTO user;
    private ReactionIconDTO reactionIcon;
    private Long threadId;
    private Long postId;
    private LocalDateTime createdAt;
}
