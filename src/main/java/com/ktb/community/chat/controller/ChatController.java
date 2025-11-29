package com.ktb.community.chat.controller;

import com.ktb.community.chat.dto.ChatRoomResDto;
import com.ktb.community.chat.dto.ChatRoomPageResponseDto;
import com.ktb.community.chat.service.ChatServiceImpl;
import com.ktb.community.dto.ApiResponseDto;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/v1/chat")
public class ChatController {
    private final ChatServiceImpl chatService;

    public ChatController(ChatServiceImpl chatService) {
        this.chatService = chatService;
    }

    //    그룹채팅방 개설
    @PostMapping("/room/group/create")
    public Mono<ApiResponseDto<Object>> createGroupRoom(@RequestParam String roomName,
                                                        @AuthenticationPrincipal(expression = "userId") Long userId){
        return chatService.createGroupRoom(roomName, userId)
                .thenReturn(ApiResponseDto.success());
    }

    //    그룹채팅목록조회
    @GetMapping("/room/group/list")
    public Mono<ApiResponseDto<ChatRoomPageResponseDto>> getGroupChatRooms(@RequestParam(name = "page", defaultValue = "0") int page){
        return chatService.getGroupChatRooms(page)
                .map(ApiResponseDto::success);
    }

    //    그룹채팅 단건 조회
    @GetMapping("/room/group")
    public Mono<ApiResponseDto<ChatRoomResDto>> getGroupChatRoomByName(@RequestParam("name") String roomName) {
        return chatService.getGroupChatRoomByName(roomName)
                .map(ApiResponseDto::success);
    }

    //    그룹채팅방참여
    @PostMapping("/room/group/{roomId}/join")
    public Mono<ApiResponseDto<Object>> joinGroupChatRoom(@PathVariable Long roomId,
                                                          @AuthenticationPrincipal(expression = "userId") Long userId){
        return chatService.addParticipantToGroupChat(roomId, userId)
                .thenReturn(ApiResponseDto.success());
    }

    //    이전 메시지 조회
    @GetMapping("/history/{roomId}")
    public Mono<ApiResponseDto<Object>> getChatHistory(@PathVariable Long roomId,
                                                       @AuthenticationPrincipal(expression = "userId") Long userId){
        return chatService.getChatHistory(roomId, userId)
                .map(ApiResponseDto::success);
    }

    //    채팅메시지 읽음처리
    @PostMapping("/room/{roomId}/read")
    public Mono<ApiResponseDto<Object>> messageRead(@PathVariable Long roomId,
                                                    @AuthenticationPrincipal(expression = "userId") Long userId){
        return chatService.messageRead(roomId, userId)
                .thenReturn(ApiResponseDto.success());
    }

    //    내채팅방목록조회 : roomId, roomName, 그룹채팅여부, 메시지읽음개수
    @GetMapping("/my/rooms")
    public Mono<ApiResponseDto<Object>> getMyChatRooms(@AuthenticationPrincipal(expression = "userId") Long userId){
        return chatService.getMyChatRooms(userId)
                .map(ApiResponseDto::success);
    }

    //    채팅방 나가기
    @DeleteMapping("/room/group/{roomId}/leave")
    public Mono<ApiResponseDto<Object>> leaveGroupChatRoom(@PathVariable Long roomId,
                                                           @AuthenticationPrincipal(expression = "userId") Long userId){
        return chatService.leaveGroupChatRoom(roomId, userId)
                .thenReturn(ApiResponseDto.success());
    }

    //    개인 채팅방 개설 또는 기존roomId return
    @PostMapping("/room/private/create")
    public Mono<ApiResponseDto<Object>> getOrCreatePrivateRoom(@RequestParam Long otherMemberId,
                                                               @AuthenticationPrincipal(expression = "userId") Long userId){
        return chatService.getOrCreatePrivateRoom(otherMemberId, userId)
                .map(ApiResponseDto::success);
    }
}
