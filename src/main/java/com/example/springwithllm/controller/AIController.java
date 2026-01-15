package com.example.springwithllm.controller;

import com.example.springwithllm.dto.ChatRequest;
import com.example.springwithllm.dto.ChatResponse;
import com.example.springwithllm.entity.ConversationHistory;
import com.example.springwithllm.service.AIModelService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
@Slf4j
public class AIController {

    private final AIModelService aiModelService;

    /**
     * Synchronous chat endpoint
     * 
     * @param request ChatRequest containing message and provider
     * @param sessionId Optional session ID for conversation tracking
     * @return ChatResponse with AI model response
     */
    @PostMapping("/chat")
    public ResponseEntity<ChatResponse> chat(
            @RequestBody ChatRequest request,
            @RequestHeader(value = "Session-Id", required = false) String sessionId) {
        
//        log.info("Received chat request");
        
        if (sessionId == null || sessionId.isEmpty()) {
            sessionId = UUID.randomUUID().toString();
            log.info("Generated new session ID: {}", sessionId);
        }
        
        ChatResponse response = aiModelService.chat(request, sessionId);
        return ResponseEntity.ok()
                .header("Session-Id", sessionId)
                .body(response);
    }

    /**
     * Streaming chat endpoint using Server-Sent Events
     * 
     * @param request ChatRequest containing message and provider
     * @param sessionId Optional session ID for conversation tracking
     * @return Flux of String tokens
     */
    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> chatStream(
            @RequestBody ChatRequest request,
            @RequestHeader(value = "Session-Id", required = false) String sessionId) {
        
        log.info("Received streaming chat request");
        
        if (sessionId == null || sessionId.isEmpty()) {
            sessionId = UUID.randomUUID().toString();
            log.info("Generated new session ID: {}", sessionId);
        }
        
        return aiModelService.chatStream(request, sessionId);
    }

    /**
     * Get conversation history for a session
     * 
     * @param sessionId Session ID to retrieve history for
     * @return List of conversation history entries
     */
    @GetMapping("/history/{sessionId}")
    public ResponseEntity<List<ConversationHistory>> getHistory(@PathVariable String sessionId) {
        log.info("Retrieving history for session: {}", sessionId);
        List<ConversationHistory> history = aiModelService.getConversationHistory(sessionId);
        return ResponseEntity.ok(history);
    }
    
    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("AI Service is running");
    }
}
