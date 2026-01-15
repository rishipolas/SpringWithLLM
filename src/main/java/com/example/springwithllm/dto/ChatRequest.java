package com.example.springwithllm.dto;

import com.example.springwithllm.enums.AIProvider;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatRequest {
    private String message;
    private AIProvider provider;
    private Double temperature;
    private Integer maxTokens;
}
