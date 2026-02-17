package com.ktb.community.chat.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "chat_messages")
@CompoundIndex(name = "room_created_idx", def = "{'roomId': 1, 'createdAt': 1}")
public class MongoChatMessage {

    @Id
    private String id;

    private Long roomId;

    private Long senderId;

    private String message;

    private String nickName;

    private Instant createdAt;
}
