package com.forum.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class UserDTO {
    private Long id;
    private String username;
    private String displayName;
    private String email;
    private String avatar;
    private String profileBanner;
    private LocalDateTime createdAt;
    private java.util.Set<String> roles;
    private Long postCount;
    private Long interactionPoints;
    private Long trophyPoints;
}
