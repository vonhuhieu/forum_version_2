package com.forum.lab.dto;

import com.forum.lab.document.LabColumnDef;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductUpdateDTO {
    private String title;
    private Map<String, Object> spec;
    private List<LabColumnDef> columns;
    private Boolean isPublic;
}
