package com.forum.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "recipient_id", nullable = false)
    private User recipient; // The person getting notified

    @ManyToOne
    @JoinColumn(name = "actor_id")
    private User actor; // The person performing action (e.g., user who commented)

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private NotificationType type;

    @ManyToOne
    @JoinColumn(name = "thread_id")
    private Thread thread;

    @ManyToOne
    @JoinColumn(name = "post_id")
    private Post post; // Linking to specific reply if exists

    @ManyToOne
    @JoinColumn(name = "conversation_id")
    private Conversation conversation; // Linking to conversation if exists

    @ManyToOne
    @JoinColumn(name = "conversation_message_id")
    private ConversationMessage conversationMessage; // Linking to conversation message if exists

    @Column(columnDefinition = "TEXT")
    private String content; // Optional brief snippet or text

    private String reactionIcon;
    private String reactionName;
    private String reactionColor;

    private boolean isRead = false;

    @CreationTimestamp
    private LocalDateTime createdAt;
}
