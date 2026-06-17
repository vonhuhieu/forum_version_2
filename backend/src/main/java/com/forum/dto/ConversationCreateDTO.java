package com.forum.dto;

import lombok.Data;
import java.util.List;

@Data
public class ConversationCreateDTO {
    private String recipientDisplayName;
    private List<String> recipientDisplayNames;
    private String title;
    private String content;
    private Boolean allowInvite;
    private Boolean locked;
}
