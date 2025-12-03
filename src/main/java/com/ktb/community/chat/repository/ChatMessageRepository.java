package com.ktb.community.chat.repository;

import com.ktb.community.chat.entity.ChatMessage;
import com.ktb.community.chat.entity.ChatRoom;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    List<ChatMessage> findByChatRoomOrderByCreatedAtAsc(ChatRoom chatRoom);

    List<ChatMessage> findTop10ByChatRoomOrderByCreatedAtDesc(ChatRoom chatRoom);
}
