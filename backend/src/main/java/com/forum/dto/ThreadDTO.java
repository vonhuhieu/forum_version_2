package com.forum.dto;

import lombok.Data;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDateTime;
import java.util.List;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ThreadDTO {
    private Long id;
    private String title;
    private String content;
    private CategoryDTO category;
    private LabelDTO label;
    private UserDTO author;
    private LocalDateTime createdAt;
    private int viewCount;
    private int replyCount;
    private boolean pinned;
    private boolean active = true;
    private boolean locked;
    private PollDTO poll;
    private String attachedImages;
    private Long lastPostId;
    private UserDTO lastPostAuthor;
    private LocalDateTime lastPostAt;
    
    // Reactions fields
    private List<ReactionSummaryDTO> reactionSummary;
    private ReactionIconDTO currentUserReaction;
    private List<UserDTO> recentReactors;
    
    private String scope;
}
