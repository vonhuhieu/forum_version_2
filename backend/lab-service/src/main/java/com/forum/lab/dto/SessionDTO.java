package com.forum.lab.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SessionDTO {
    private String sessionId;
    private String title;
    private String status;
    private String productId;
    private Instant createdAt;
    private Instant updatedAt;
}
