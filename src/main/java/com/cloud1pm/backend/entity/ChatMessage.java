package com.cloud1pm.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "chat_messages")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    @Column(nullable = false)
    private Boolean isUserMessage; // true: 사용자 메시지, false: 챗봇 응답

    @Column
    private String sentiment; // positive, negative, neutral

    @Column
    private Double sentimentScore; // 감정 점수

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;
}