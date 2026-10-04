package com.forum.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "thread_subscriptions", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"thread_id", "user_id"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ThreadSubscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "thread_id", nullable = false)
    private Thread thread;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "is_following", nullable = false)
    private boolean isFollowing;
}
