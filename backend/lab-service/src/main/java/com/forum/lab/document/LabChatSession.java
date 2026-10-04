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

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "lab_chat_sessions")
@CompoundIndex(name = "owner_updated_idx", def = "{'ownerUsername': 1, 'updatedAt': -1}")
public class LabChatSession {

    @Id
    private String id;

    @Indexed(unique = true)
    private String sessionId;

    @Indexed
    private String ownerUsername;

    private String title;

    private String status;

    private String productId;

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;

    @Indexed(expireAfter = "0s")
    private Instant expiresAt;
}
