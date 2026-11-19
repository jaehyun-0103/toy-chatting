package com.example.toychat.repository;

import com.example.toychat.entity.ChatRoom;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {

    // 생성자 ID로 채팅방 존재 여부 확인
    boolean existsByCreator_Id(Long creatorId);

    // 비관적 락을 사용하여 채팅방 ID로 채팅방 조회
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM ChatRoom c WHERE c.id = :id")
    Optional<ChatRoom> findByIdWithLock(@Param("id") Long id);
}
