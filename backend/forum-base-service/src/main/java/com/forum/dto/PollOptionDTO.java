package com.forum.dto;

import lombok.Data;

@Data
public class PollOptionDTO {
    private Long id;
    private String optionText;
    private int voteCount;
    private double percentage;
}
