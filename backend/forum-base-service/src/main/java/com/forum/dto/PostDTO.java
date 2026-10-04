package com.forum.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class PostDTO {
    private Long id;
    private String content;
    private UserDTO author;
    private Long threadId; // for mapping
    private LocalDateTime createdAt;
    private String attachedImages;
    private Boolean autoFollowed;
    
    private String threadTitle;
    private LabelDTO threadLabel;
    private CategoryDTO category;
    private Long seqNumber;
    
    // Reactions fields
    private List<ReactionSummaryDTO> reactionSummary;
    private ReactionIconDTO currentUserReaction;
    private List<UserDTO> recentReactors;
}
