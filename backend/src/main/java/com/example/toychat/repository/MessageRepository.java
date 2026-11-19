package com.example.toychat.repository;

import com.example.toychat.dto.response.MessageResponseDTO;
import com.example.toychat.entity.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MessageRepository extends JpaRepository<Message, Long> {

    // 채팅방 ID로 메시지 응답 목록을 오름차순으로 조회
    @Query("""
            SELECT new com.example.toychat.dto.response.MessageResponseDTO(
                m.id,
                u.username,
                u.id,
                m.content,
                m.updatedAt
            )
            FROM Message m
            JOIN m.user u
            WHERE m.chatRoom.id = :chatroomId
            ORDER BY m.createdAt ASC, m.id ASC
            """)
    List<MessageResponseDTO> findMessageResponsesByChatRoomId(
            @Param("chatroomId") Long chatroomId
    );
}