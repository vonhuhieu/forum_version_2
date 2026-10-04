package com.forum.lab.dto;

import com.forum.lab.document.LabMessage;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageDTO {
    private String id;
    private String sessionId;
    private String role;
    private String content;
    private LabMessage.ProductCard productCard;
    private Instant createdAt;
}
