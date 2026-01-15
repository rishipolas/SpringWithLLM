package com.example.springwithllm.repository;

import com.example.springwithllm.entity.ConversationHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ConversationHistoryRepository extends JpaRepository<ConversationHistory, Long> {
    List<ConversationHistory> findBySessionIdOrderByTimestampAsc(String sessionId);
}
