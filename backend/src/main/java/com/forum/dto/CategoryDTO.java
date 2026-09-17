package com.forum.dto;

import lombok.Data;

@Data
public class CategoryDTO {
    private Long id;
    private String name;
    private String description;
    private String icon;
    private Integer positionOrder;
    private boolean active;
    private Long categoryGroupId;
    private Long parentCategoryId;
    private Long threadCount;
    private Long postCount;
    private java.util.List<CategoryDTO> subCategories;
}
