package com.forum.dto;

import lombok.Data;

@Data
public class MenuDTO {
    private Long id;
    private String title;
    private String url;
    private Integer positionOrder;
    private boolean active;
}
