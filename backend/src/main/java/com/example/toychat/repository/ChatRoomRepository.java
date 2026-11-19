package com.example.toychat.repository;

import com.example.toychat.entity.ChatRoom;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {

    // 생성자 ID로 채팅방 존재 여부 확인
    boolean existsByCreator_Id(Long creatorId);
}
