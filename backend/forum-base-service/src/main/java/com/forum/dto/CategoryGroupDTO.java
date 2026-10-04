package com.forum.dto;

import lombok.Data;
import java.util.List;

@Data
public class CategoryGroupDTO {
    private Long id;
    private String name;
    private Integer positionOrder;
    private boolean active;
    private List<CategoryDTO> categories;
}
