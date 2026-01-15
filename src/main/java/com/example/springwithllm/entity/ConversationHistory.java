package com.example.springwithllm.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "conversation_history")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationHistory {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String sessionId;
    
    @Column(nullable = false, columnDefinition = "TEXT")
    private String userMessage;
    
    @Column(nullable = false, columnDefinition = "TEXT")
    private String aiResponse;
    
    @Column(nullable = false)
    private String provider;
    
    @Column(nullable = false)
    private LocalDateTime timestamp;
    
    private Integer tokensUsed;
}
