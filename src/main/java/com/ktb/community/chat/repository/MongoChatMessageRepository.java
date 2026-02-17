package com.ktb.community.chat.repository;

import com.ktb.community.chat.document.MongoChatMessage;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface MongoChatMessageRepository extends MongoRepository<MongoChatMessage, String> {
    List<MongoChatMessage> findByRoomIdOrderByCreatedAtAsc(Long roomId);
}
