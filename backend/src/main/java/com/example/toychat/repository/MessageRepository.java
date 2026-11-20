package com.example.toychat.repository;

import com.example.toychat.dto.response.MessageResponseDTO;
import com.example.toychat.entity.Message;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MessageRepository extends JpaRepository<Message, Long> {

    // 채팅방 ID로 최신 메시지 응답 목록을 내림차순으로 조회
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
            ORDER BY m.id DESC
            """)
    Slice<MessageResponseDTO> findRecentMessageResponses(
            @Param("chatroomId") Long chatroomId,
            Pageable pageable
    );

    // 채팅방 ID와 기준 메시지 ID로 이전 메시지 응답 목록을 내림차순으로 조회
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
            AND m.id < :beforeId
            ORDER BY m.id DESC
            """)
    Slice<MessageResponseDTO> findOlderMessageResponses(
            @Param("chatroomId") Long chatroomId,
            @Param("beforeId") Long beforeId,
            Pageable pageable
    );
}