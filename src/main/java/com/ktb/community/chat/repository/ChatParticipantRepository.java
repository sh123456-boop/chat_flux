package com.ktb.community.chat.repository;

import com.ktb.community.chat.entity.ChatParticipant;
import com.ktb.community.chat.entity.ChatRoom;
import com.ktb.community.entity.User;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ChatParticipantRepository extends JpaRepository<ChatParticipant, Long> {

    List<ChatParticipant> findByChatRoom(ChatRoom chatRoom);
    Optional<ChatParticipant> findByChatRoomAndUser(ChatRoom chatRoom, User user);
    List<ChatParticipant> findAllByUser(User user);


    @Query("SELECT cp1.chatRoom FROM ChatParticipant cp1 JOIN ChatParticipant cp2 ON cp1.chatRoom.id = cp2.chatRoom.id WHERE cp1.user.id = :myId AND cp2.user.id = :otherMemberId AND cp1.chatRoom.isGroupChat = false")
    Optional<ChatRoom> findExistingPrivateRoom(@Param("myId") Long myId, @Param("otherMemberId") Long otherMemberId);
}
