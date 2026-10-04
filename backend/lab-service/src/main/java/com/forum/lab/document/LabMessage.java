package com.forum.lab.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "lab_messages")
@CompoundIndex(name = "session_created_idx", def = "{'sessionId': 1, 'createdAt': 1}")
public class LabMessage {

    @Id
    private String id;

    @Indexed
    private String sessionId;

    private String role;

    private String content;

    private ProductCard productCard;

    @CreatedDate
    private Instant createdAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProductCard {
        private String publicId;
        private String title;
        private String templateType;
    }
}
