package com.ktb.community.chat.service;

import com.ktb.community.chat.dto.*;
import com.ktb.community.chat.entity.ChatRoom;
import com.ktb.community.entity.User;
import reactor.core.publisher.Mono;

import java.util.List;

public interface ChatService {
    // 메시지 발행 시 메시지 저장
    Mono<Void> saveMessage(Long roomId, ChatMessageReqDto chatMessageReqDto);

    // 그룹 채팅방 생성
    Mono<Void> createGroupRoom(String chatRoomName, Long userId);

    // 그룹 채팅방 조회
    Mono<ChatRoomPageResponseDto> getGroupChatRooms(int page);

    // 채팅방 이름으로 단일 조회
    Mono<ChatRoomResDto> getGroupChatRoomByName(String roomName);

    // 그룹채팅방 참여
    Mono<Void> addParticipantToGroupChat(Long roomId, Long userId);

    // 채팅방 참여
    Mono<Void> addParticipantToRoom(ChatRoom chatRoom, User user);

    // 채팅방 이전 메시지 조회
    Mono<List<ChatMessageDto>> getChatHistory(Long roomId, Long userId);

    // 유저가 해당 채팅방 참여자인지 확인
    Mono<Boolean> isRoomParticipant(Long userId, Long roomId);

    // 메시지 읽음 처리
    Mono<Void> messageRead(Long roomId, Long userId);

    // 내 채팅 목록 조회
    Mono<List<MyChatListResDto>> getMyChatRooms(Long userId);

    // 그룹 채팅방 나가기
    Mono<Void> leaveGroupChatRoom(Long roomId, Long userId);

    // 1:1 채팅방 개설
    Mono<Long> getOrCreatePrivateRoom(Long otherMemberId, Long userId);
}
