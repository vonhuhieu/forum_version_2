package com.forum.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class PollDTO {
    private Long id;
    private String question;
    private int maxChoices;
    private boolean allowChangeVote;
    private boolean publicVoting;
    private boolean showResultWithoutVote;
    private LocalDateTime closedAt;
    private List<PollOptionDTO> options;
    private int totalVotes;
    // ID option mà user hiện tại đã vote (null nếu chưa vote)
    private List<Long> userVotedOptionIds;
}
