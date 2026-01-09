package com.ssg9th2team.geharbang.domain.chat.dto;

import com.ssg9th2team.geharbang.domain.chat.RealtimeChatRoom;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class ChatRoomDto {
    private Long id;
    private Long reservationId;
    private Long accommodationId;
    private String accommodationName;
    private String accommodationImage;
    private Long hostUserId;
    private Long guestUserId;
    private String otherParticipantName; // The name of the other user in the chat
    private String lastMessage;
    private LocalDateTime lastMessageTime;
    private Integer unreadCount; // Dynamic unread count for the current user

    public static ChatRoomDto of(RealtimeChatRoom room, String otherParticipantName, int unreadCount) {
        return ChatRoomDto.builder()
                .id(room.getId())
                .reservationId(room.getReservationId())
                .accommodationId(room.getAccommodationId())
                .accommodationName(room.getAccommodationName())
                .accommodationImage(room.getAccommodationImage())
                .hostUserId(room.getHostUserId())
                .guestUserId(room.getGuestUserId())
                .otherParticipantName(otherParticipantName)
                .lastMessage(room.getLastMessage())
                .lastMessageTime(room.getLastMessageTime())
                .unreadCount(unreadCount)
                .build();
    }
}
