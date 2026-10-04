package com.forum.lab.template;

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
public class TemplateDefinition {
    private String templateType;
    private String name;
    private String description;
    private String icon;
    private List<LabColumnDef> defaultColumns;
    private Map<String, Object> defaultSpec;
    private List<Map<String, Object>> mockRows;
}
