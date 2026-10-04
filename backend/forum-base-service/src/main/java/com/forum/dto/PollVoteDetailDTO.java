package com.forum.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PollVoteDetailDTO {
    private Long id;
    private String username;
    private String displayName;
    private Long optionId;
    private String optionText;
}
