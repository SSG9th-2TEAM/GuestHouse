package com.ssg9th2team.geharbang.domain.chat.service;

import com.ssg9th2team.geharbang.domain.chat.RealtimeChatMessage;
import com.ssg9th2team.geharbang.domain.chat.RealtimeChatRoom;
import com.ssg9th2team.geharbang.domain.chat.dto.ChatMessageDto;
import com.ssg9th2team.geharbang.domain.chat.dto.ChatRoomDto;
import com.ssg9th2team.geharbang.domain.chat.repository.RealtimeChatMessageRepository;
import com.ssg9th2team.geharbang.domain.chat.repository.RealtimeChatRoomRepository;
import com.ssg9th2team.geharbang.domain.auth.entity.User;
import com.ssg9th2team.geharbang.domain.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Service
@RequiredArgsConstructor
public class RealtimeChatService {

    private final RealtimeChatRoomRepository chatRoomRepository;
    private final RealtimeChatMessageRepository chatMessageRepository;
    private final UserRepository userRepository;

    // 유저의 채팅방 목록 조회
    @Transactional(readOnly = true)
    public List<ChatRoomDto> getUserChatRooms(Long userId) {
        log.info("사용자 채팅방 목록 조회 시작. 사용자 ID: {}", userId);
        List<RealtimeChatRoom> rooms = chatRoomRepository.findByGuestUserIdOrHostUserId(userId, userId);
        log.info("사용자 ID {}에 대해 {}개의 채팅방을 찾았습니다.", userId, rooms.size());

        if (rooms.isEmpty()) {
            return Collections.emptyList();
        }

        Set<Long> otherUserIds = rooms.stream()
                .map(room -> {
                    if (userId.equals(room.getGuestUserId())) {
                        return room.getHostUserId();
                    } else {
                        return room.getGuestUserId();
                    }
                })
                .collect(Collectors.toSet());
        log.info("상대방 사용자 이름 조회를 위한 ID 목록: {}", otherUserIds);


        Map<Long, User> userMap = userRepository.findAllById(otherUserIds).stream()
                .collect(Collectors.toMap(User::getId, user -> user));
      log.info("상대방 사용자 정보 {}건을 찾았습니다.", userMap.size());


        List<ChatRoomDto> chatRoomDtos = rooms.stream().map(room -> {
            Long otherUserId;
            String otherUserName;
            int unreadCount;

            if (userId.equals(room.getGuestUserId())) {
                otherUserId = room.getHostUserId();
                unreadCount = room.getGuestUnreadCount();
            } else {
                otherUserId = room.getGuestUserId();
                unreadCount = room.getHostUnreadCount();
            }

            User otherUser = userMap.get(otherUserId);
            otherUserName = (otherUser != null) ? otherUser.getNickname() : "알 수 없는 사용자";

            return ChatRoomDto.of(room, otherUserName, unreadCount);
        }).collect(Collectors.toList());

        log.info("사용자 ID {}에게 {}개의 채팅방 DTO를 반환합니다.", userId, chatRoomDtos.size());
        return chatRoomDtos;
    }

    // 특정 채팅방의 메시지 조회
    @Transactional(readOnly = true)
    public List<ChatMessageDto> getRoomMessages(Long roomId, Long currentUserId) {
        RealtimeChatRoom chatRoom = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("Chat room not found"));

        // 사용자가 해당 채팅방의 참여자인지 확인
        if (!chatRoom.getHostUserId().equals(currentUserId) && !chatRoom.getGuestUserId().equals(currentUserId)) {
            throw new SecurityException("User is not a participant of this chat room");
        }

        return chatMessageRepository.findByChatRoomIdOrderByCreatedAtAsc(roomId).stream()
                .map(ChatMessageDto::fromEntity)
                .collect(Collectors.toList());
    }

    // 메시지 저장
    @Transactional
    public ChatMessageDto saveMessage(Long roomId, Long senderUserId, String content) {
        RealtimeChatRoom chatRoom = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("Chat room not found"));

        User sender = userRepository.findById(senderUserId)
                .orElseThrow(() -> new IllegalArgumentException("Sender not found"));

        // 메시지 엔티티 생성
        RealtimeChatMessage message = RealtimeChatMessage.builder()
                .chatRoomId(roomId)
                .senderUserId(senderUserId)
                .senderName(sender.getName())
                .messageContent(content)
                .createdAt(LocalDateTime.now())
                .isRead(false) // 초기에는 읽지 않은 상태
                .build();

        RealtimeChatMessage savedMessage = chatMessageRepository.save(message);

        // 채팅방 정보 업데이트 (마지막 메시지, 시간, 안 읽은 메시지 수)
        chatRoom.setLastMessage(content);
        chatRoom.setLastMessageTime(message.getCreatedAt());

        // 메시지를 보낸 사람이 아닌 다른 참여자의 unreadCount 증가
        if (chatRoom.getHostUserId().equals(senderUserId)) {
            chatRoom.setGuestUnreadCount(chatRoom.getGuestUnreadCount() + 1);
        } else if (chatRoom.getGuestUserId().equals(senderUserId)) {
            chatRoom.setHostUnreadCount(chatRoom.getHostUnreadCount() + 1);
        }
        chatRoom.setUpdatedAt(LocalDateTime.now());
        chatRoomRepository.save(chatRoom);

        return ChatMessageDto.fromEntity(savedMessage);
    }

    // 메시지 읽음 처리
    @Transactional
    public void markMessagesAsRead(Long roomId, Long readerUserId) {
        RealtimeChatRoom chatRoom = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("Chat room not found"));

        // 사용자가 해당 채팅방의 참여자인지 확인
        if (!chatRoom.getHostUserId().equals(readerUserId) && !chatRoom.getGuestUserId().equals(readerUserId)) {
            throw new SecurityException("User is not a participant of this chat room");
        }

        // 해당 채팅방에서 현재 유저가 보낸 메시지를 제외하고 읽지 않은 메시지를 모두 읽음 처리
        chatMessageRepository.markMessagesAsReadForRoomAndUser(roomId, readerUserId);

        // 채팅방의 unreadCount를 0으로 초기화
        if (chatRoom.getHostUserId().equals(readerUserId)) {
            chatRoom.setHostUnreadCount(0);
        } else if (chatRoom.getGuestUserId().equals(readerUserId)) {
            chatRoom.setGuestUnreadCount(0);
        }
        chatRoomRepository.save(chatRoom);
    }
}
