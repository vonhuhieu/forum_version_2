package com.forum.lab.dto;

import com.forum.lab.document.LabColumnDef;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductDTO {
    private String publicId;
    private String sessionId;
    private String ownerUsername;
    private String title;
    private String templateType;
    private Map<String, Object> spec;
    private List<LabColumnDef> columns;
    private boolean isPublic;
    private boolean isSaved;
    private int version;
    private Instant createdAt;
    private Instant updatedAt;
}
