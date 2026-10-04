package com.forum.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LabelDTO {
    private Long id;
    private String name;
    private String colorCode;
    private String textColor;
    private String borderColor;
    private Boolean adminOnly;
}
