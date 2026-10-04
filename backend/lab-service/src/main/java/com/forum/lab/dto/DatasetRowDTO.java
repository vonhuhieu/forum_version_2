package com.forum.lab.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DatasetRowDTO {
    private String id;
    private String productId;
    private int orderIndex;
    private Map<String, Object> data;
    private Instant createdAt;
    private Instant updatedAt;
}
