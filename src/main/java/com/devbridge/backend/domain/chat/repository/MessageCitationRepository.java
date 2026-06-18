package com.devbridge.backend.domain.chat.repository;

import com.devbridge.backend.domain.chat.entity.MessageCitation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MessageCitationRepository extends JpaRepository<MessageCitation, String> {
}
