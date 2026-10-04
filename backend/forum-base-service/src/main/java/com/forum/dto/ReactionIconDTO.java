package com.forum.dto;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReactionIconDTO {
    private Long id;
    private String icon;
    private String tooltip;
    private String color;
    private int orderIndex;
}
