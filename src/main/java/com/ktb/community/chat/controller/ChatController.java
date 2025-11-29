package com.ktb.community.chat.controller;

import com.ktb.community.chat.dto.ChatMessageDto;
import com.ktb.community.chat.dto.ChatRoomResDto;
import com.ktb.community.chat.dto.ChatRoomPageResponseDto;
import com.ktb.community.chat.dto.MyChatListResDto;
import com.ktb.community.chat.service.ChatServiceImpl;
import com.ktb.community.dto.ApiResponseDto;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v1/chat")
public class ChatController {
    private final ChatServiceImpl chatService;

    public ChatController(ChatServiceImpl chatService) {
        this.chatService = chatService;
    }

    //    그룹채팅방 개설
    @PostMapping("/room/group/create")
    public ApiResponseDto<Object> createGroupRoom(@RequestParam String roomName,
                                             @AuthenticationPrincipal(expression = "userId") Long userId){
        chatService.createGroupRoom(roomName,userId);
        return ApiResponseDto.success();
    }

    //    그룹채팅목록조회
    @GetMapping("/room/group/list")
    public ApiResponseDto<ChatRoomPageResponseDto> getGroupChatRooms(@RequestParam(name = "page", defaultValue = "0") int page){
        ChatRoomPageResponseDto chatRooms = chatService.getGroupChatRooms(page);
        return ApiResponseDto.success(chatRooms);
    }

    //    그룹채팅 단건 조회
    @GetMapping("/room/group")
    public ApiResponseDto<ChatRoomResDto> getGroupChatRoomByName(@RequestParam("name") String roomName) {
        ChatRoomResDto chatRoom = chatService.getGroupChatRoomByName(roomName);
        return ApiResponseDto.success(chatRoom);
    }

    //    그룹채팅방참여
    @PostMapping("/room/group/{roomId}/join")
    public ApiResponseDto<Object> joinGroupChatRoom(@PathVariable Long roomId,
                                               @AuthenticationPrincipal(expression = "userId") Long userId){
        chatService.addParticipantToGroupChat(roomId, userId);
        return ApiResponseDto.success();
    }

    //    이전 메시지 조회
    @GetMapping("/history/{roomId}")
    public ApiResponseDto<Object> getChatHistory(@PathVariable Long roomId,
                                            @AuthenticationPrincipal(expression = "userId") Long userId){
        List<ChatMessageDto> chatMessageDtos = chatService.getChatHistory(roomId, userId);
        return ApiResponseDto.success(chatMessageDtos);
    }

    //    채팅메시지 읽음처리
    @PostMapping("/room/{roomId}/read")
    public ApiResponseDto<Object> messageRead(@PathVariable Long roomId,
                                         @AuthenticationPrincipal(expression = "userId") Long userId){
        chatService.messageRead(roomId, userId);
        return ApiResponseDto.success();
    }

    //    내채팅방목록조회 : roomId, roomName, 그룹채팅여부, 메시지읽음개수
    @GetMapping("/my/rooms")
    public ApiResponseDto<Object> getMyChatRooms(@AuthenticationPrincipal(expression = "userId") Long userId){
        List<MyChatListResDto> myChatListResDtos = chatService.getMyChatRooms(userId);
        return ApiResponseDto.success(myChatListResDtos);
    }

    //    채팅방 나가기
    @DeleteMapping("/room/group/{roomId}/leave")
    public ApiResponseDto<Object> leaveGroupChatRoom(@PathVariable Long roomId,
                                                @AuthenticationPrincipal(expression = "userId") Long userId){
        chatService.leaveGroupChatRoom(roomId, userId);
        return ApiResponseDto.success();
    }

    //    개인 채팅방 개설 또는 기존roomId return
    @PostMapping("/room/private/create")
    public ApiResponseDto<Object> getOrCreatePrivateRoom(@RequestParam Long otherMemberId,
                                                    @AuthenticationPrincipal(expression = "userId") Long userId){
        Long roomId = chatService.getOrCreatePrivateRoom(otherMemberId, userId);
        return ApiResponseDto.success(roomId);
    }
}
