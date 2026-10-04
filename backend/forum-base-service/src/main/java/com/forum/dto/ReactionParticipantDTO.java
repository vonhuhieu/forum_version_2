package com.forum.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReactionParticipantDTO {
    private UserDTO user;
    private ReactionIconDTO reactionIcon;
    private LocalDateTime interactedAt;
}
