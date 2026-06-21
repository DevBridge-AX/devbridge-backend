package com.devbridge.backend.domain.chat.repository;

import com.devbridge.backend.domain.chat.entity.ChatSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatSessionRepository extends JpaRepository<ChatSession, String> {

    @Query("SELECT s, (SELECT COALESCE(MAX(m.createdAt), s.createdAt) FROM ChatMessage m WHERE m.session = s) " +
           "FROM ChatSession s " +
           "WHERE s.workspace.id = :workspaceId AND s.user.employeeId = :employeeId " +
           "ORDER BY (SELECT COALESCE(MAX(m.createdAt), s.createdAt) FROM ChatMessage m WHERE m.session = s) DESC")
    List<Object[]> findSessionsSortedByLastMessage(
            @Param("workspaceId") String workspaceId,
            @Param("employeeId") String employeeId
    );
}
