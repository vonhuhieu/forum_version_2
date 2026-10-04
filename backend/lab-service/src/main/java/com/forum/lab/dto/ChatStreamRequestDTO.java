package com.forum.lab.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatStreamRequestDTO {
    private String sessionId;

    @NotBlank(message = "Nội dung tin nhắn không được để trống")
    private String message;

    private String turnstileToken;
}
