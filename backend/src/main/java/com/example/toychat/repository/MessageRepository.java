package com.example.toychat.repository;

import com.example.toychat.entity.ChatRoom;
import com.example.toychat.entity.Message;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MessageRepository extends JpaRepository<Message, Long> {

    List<Message> findByChatRoomOrderByCreatedAtAsc(ChatRoom chatRoom); // 특정 채팅방의 모든 메시지를 생성된 시간순으로 오름차순 정렬하여 조회
}