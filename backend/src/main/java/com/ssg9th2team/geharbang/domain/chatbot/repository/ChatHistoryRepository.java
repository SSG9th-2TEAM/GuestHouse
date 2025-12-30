package com.ssg9th2team.geharbang.domain.chatbot.repository;

import com.ssg9th2team.geharbang.domain.chatbot.entity.ChatHistory;
import com.ssg9th2team.geharbang.domain.chatbot.entity.ChatRoom;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChatHistoryRepository extends JpaRepository<ChatHistory, Long> {
    List<ChatHistory> findByChatRoomOrderByCreatedAtAsc(ChatRoom chatRoom);

    void deleteByChatRoom(ChatRoom chatRoom);
}
