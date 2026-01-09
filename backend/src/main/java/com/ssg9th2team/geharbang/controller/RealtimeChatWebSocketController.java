package com.ssg9th2team.geharbang.controller;

import com.ssg9th2team.geharbang.domain.auth.entity.User;
import com.ssg9th2team.geharbang.domain.auth.repository.UserRepository;
import com.ssg9th2team.geharbang.domain.chat.dto.ChatMessageDto;
import com.ssg9th2team.geharbang.domain.chat.dto.ChatMessageRequest;
import com.ssg9th2team.geharbang.domain.chat.service.RealtimeChatService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class RealtimeChatWebSocketController {

    private static final Logger log = LoggerFactory.getLogger(RealtimeChatWebSocketController.class);
    private final RealtimeChatService chatService;
    private final SimpMessagingTemplate messagingTemplate;
    private final UserRepository userRepository;

    @MessageMapping("/chat/{roomId}/send")
    public void sendMessage(
            @DestinationVariable Long roomId,
            @Payload ChatMessageRequest request,
            SimpMessageHeaderAccessor headerAccessor) {

        // 메시지 헤더에서 인증 정보 가져오기
        Authentication authentication = (Authentication) headerAccessor.getUser();

        log.info("Received message for room {}, user: {}, content: {}",
                roomId, authentication != null ? authentication.getName() : "NULL", request.getContent());

        if (authentication == null || !authentication.isAuthenticated()) {
            log.warn("Unauthenticated user attempted to send message to room {}", roomId);
            return;
        }

        try {
            String userEmail = authentication.getName();
            User user = userRepository.findByEmail(userEmail)
                    .orElseThrow(() -> new UsernameNotFoundException("User not found: " + userEmail));
            Long senderUserId = user.getId();

            log.info("Saving message from user {} (ID: {}) to room {}", user.getEmail(), senderUserId, roomId);
            ChatMessageDto message = chatService.saveMessage(roomId, senderUserId, request.getContent());

            log.info("Broadcasting message to /topic/chatroom/{}", roomId);
            messagingTemplate.convertAndSend("/topic/chatroom/" + roomId, message);

            log.info("Message successfully sent to room {}", roomId);
        } catch (Exception e) {
            log.error("Error sending message in room {}: {}", roomId, e.getMessage(), e);
        }
    }
}