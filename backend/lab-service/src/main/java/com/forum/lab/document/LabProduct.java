package com.forum.lab.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "lab_products")
@CompoundIndex(name = "owner_updated_idx", def = "{'ownerUsername': 1, 'updatedAt': -1}")
public class LabProduct {

    @Id
    private String id;

    @Indexed(unique = true)
    private String publicId;

    @Indexed
    private String sessionId;

    @Indexed
    private String ownerUsername;

    private String title;

    private String templateType;

    private Map<String, Object> spec;

    private List<LabColumnDef> columns;

    private boolean isPublic;

    private boolean isSaved;

    private int version;

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;

    @Indexed(expireAfter = "0s")
    private Instant expiresAt;
}
