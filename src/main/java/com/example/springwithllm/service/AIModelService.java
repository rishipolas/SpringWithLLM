package com.example.springwithllm.service;

import com.example.springwithllm.dto.ChatRequest;
import com.example.springwithllm.dto.ChatResponse;
import com.example.springwithllm.entity.ConversationHistory;
import com.example.springwithllm.enums.AIProvider;
import com.example.springwithllm.repository.ConversationHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.anthropic.AnthropicChatModel;
import org.springframework.ai.azure.openai.AzureOpenAiChatModel;
import org.springframework.ai.vertexai.gemini.VertexAiGeminiChatModel;
import org.springframework.ai.bedrock.anthropic.BedrockAnthropicChatModel;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AIModelService {

    private final ConversationHistoryRepository conversationHistoryRepository;
    
    @Autowired(required = false)
    private OpenAiChatModel openAiChatModel;
    
    @Autowired(required = false)
    private AnthropicChatModel anthropicChatModel;
    
    @Autowired(required = false)
    private AzureOpenAiChatModel azureOpenAiChatModel;
    
    @Autowired(required = false)
    private VertexAiGeminiChatModel vertexAiChatModel;
    
    @Autowired(required = false)
    private BedrockAnthropicChatModel bedrockChatModel;
    
    @Autowired(required = false)
    private OllamaChatModel ollamaChatModel;

    /**
     * Synchronous chat completion
     */
    public ChatResponse chat(ChatRequest request, String sessionId) {
        log.info("Processing chat request with provider: {}", request.getProvider());
        
        ChatModel chatModel = getChatModel(request.getProvider());
        
        ChatClient chatClient = ChatClient.builder(chatModel).build();
        
        String response = chatClient.prompt()
                .user(request.getMessage())
                .call()
                .content();
        
        // Save conversation history
        ConversationHistory history = ConversationHistory.builder()
                .sessionId(sessionId)
                .userMessage(request.getMessage())
                .aiResponse(response)
                .provider(request.getProvider().name())
                .timestamp(LocalDateTime.now())
                .build();
        
        conversationHistoryRepository.save(history);
        
        log.info("Chat completed successfully");
        
        return ChatResponse.builder()
                .response(response)
                .provider(request.getProvider().name())
                .timestamp(LocalDateTime.now())
                .build();
    }
    
    /**
     * Streaming chat completion
     */
    public Flux<String> chatStream(ChatRequest request, String sessionId) {
        log.info("Processing streaming chat request with provider: {}", request.getProvider());
        
        ChatModel chatModel = getChatModel(request.getProvider());
        
        ChatClient chatClient = ChatClient.builder(chatModel).build();
        
        StringBuilder fullResponse = new StringBuilder();
        
        return chatClient.prompt()
                .user(request.getMessage())
                .stream()
                .content()
                .doOnNext(fullResponse::append)
                .doOnComplete(() -> {
                    // Save conversation after streaming completes
                    ConversationHistory history = ConversationHistory.builder()
                            .sessionId(sessionId)
                            .userMessage(request.getMessage())
                            .aiResponse(fullResponse.toString())
                            .provider(request.getProvider().name())
                            .timestamp(LocalDateTime.now())
                            .build();
                    conversationHistoryRepository.save(history);
                    log.info("Streaming chat completed successfully");
                });
    }
    
    /**
     * Get chat model based on provider
     */
    private ChatModel getChatModel(AIProvider provider) {
        return switch (provider) {
            case OPENAI -> {
                if (openAiChatModel == null) {
                    throw new IllegalStateException("OpenAI model is not configured. Please set OPENAI_API_KEY");
                }
                yield openAiChatModel;
            }
            case ANTHROPIC -> {
                if (anthropicChatModel == null) {
                    throw new IllegalStateException("Anthropic model is not configured. Please set ANTHROPIC_API_KEY");
                }
                yield anthropicChatModel;
            }
            case AZURE_OPENAI -> {
                if (azureOpenAiChatModel == null) {
                    throw new IllegalStateException("Azure OpenAI model is not configured. Please set AZURE_OPENAI_API_KEY");
                }
                yield azureOpenAiChatModel;
            }
            case VERTEX_AI -> {
                if (vertexAiChatModel == null) {
                    throw new IllegalStateException("Vertex AI model is not configured. Please set VERTEX_AI_PROJECT_ID");
                }
                yield vertexAiChatModel;
            }
            case BEDROCK -> {
                if (bedrockChatModel == null) {
                    throw new IllegalStateException("Bedrock model is not configured. Please set AWS credentials");
                }
                yield bedrockChatModel;
            }
            case OLLAMA -> {
                if (ollamaChatModel == null) {
                    throw new IllegalStateException("Ollama model is not configured. Please ensure Ollama is running");
                }
                yield ollamaChatModel;
            }
        };
    }
    
    /**
     * Get conversation history for a session
     */
    public List<ConversationHistory> getConversationHistory(String sessionId) {
        return conversationHistoryRepository.findBySessionIdOrderByTimestampAsc(sessionId);
    }
}
