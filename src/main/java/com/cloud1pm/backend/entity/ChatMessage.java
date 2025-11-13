// src/main/java/com/cloud1pm/backend/entity/ChatMessage.java
package com.cloud1pm.backend.entity;

import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "chat_messages")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private ChatSession session;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    @Column(name = "is_user_message", nullable = false)
    private Boolean isUserMessage;

    @Column(length = 20)
    private String sentiment;

    @Column(name = "sentiment_score")
    private Double sentimentScore;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;
}