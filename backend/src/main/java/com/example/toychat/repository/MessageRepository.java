package com.example.toychat.repository;

import com.example.toychat.entity.ChatRoom;
import com.example.toychat.entity.Message;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MessageRepository extends JpaRepository<Message, Long> {
    // 채팅방의 메시지를 오름차순으로 조회
    List<Message> findByChatRoomOrderByCreatedAtAsc(ChatRoom chatRoom);
}