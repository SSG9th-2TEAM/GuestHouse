package com.ssg9th2team.geharbang.controller;

import com.ssg9th2team.geharbang.domain.auth.entity.User;
import com.ssg9th2team.geharbang.domain.auth.repository.UserRepository;
import com.ssg9th2team.geharbang.domain.chat.dto.ChatMessageDto;
import com.ssg9th2team.geharbang.domain.chat.dto.ChatRoomDto;
import com.ssg9th2team.geharbang.domain.chat.service.RealtimeChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/realtime-chat")
@RequiredArgsConstructor
public class RealtimeChatController {

    private final RealtimeChatService chatService;
    private final UserRepository userRepository; // UserRepository 주입

    // 내 채팅방 목록 조회
    @GetMapping("/rooms")
    public ResponseEntity<List<ChatRoomDto>> getMyChatRooms(@AuthenticationPrincipal UserDetails userDetails) {
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        Long userId = user.getId();
        return ResponseEntity.ok(chatService.getUserChatRooms(userId));
    }

    // 특정 채팅방 메시지 조회
    @GetMapping("/rooms/{roomId}/messages")
    public ResponseEntity<List<ChatMessageDto>> getRoomMessages(
            @PathVariable Long roomId,
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        Long userId = user.getId();
        return ResponseEntity.ok(chatService.getRoomMessages(roomId, userId));
    }

    // 메시지 읽음 처리
    @PostMapping("/rooms/{roomId}/read")
    public ResponseEntity<Void> markAsRead(
            @PathVariable Long roomId,
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        Long userId = user.getId();
        chatService.markMessagesAsRead(roomId, userId);
        return ResponseEntity.ok().build();
    }
}
