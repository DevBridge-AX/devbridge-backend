package com.devbridge.backend.domain.chat.repository;

import com.devbridge.backend.domain.chat.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, String> {

    List<ChatMessage> findBySession_IdOrderByCreatedAtAsc(String sessionId);
}
