package com.example.toychat.repository;

import com.example.toychat.entity.ChatRoom;
import com.example.toychat.entity.InviteCode;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface InviteCodeRepository extends JpaRepository<InviteCode, Long> {
    Optional<InviteCode> findByChatRoom(ChatRoom chatRoom); // 주어진 채팅방에 대한 초대 코드를 조회
    Optional<InviteCode> findByInviteCode(String inviteCode); // 주어진 초대 코드를 통해 초대 코드 정보를 조회

    List<InviteCode> findByExpirationDateBefore(LocalDateTime dateTime); // 만료된 초대 코드를 검색
    @Modifying
    @Query("DELETE FROM InviteCode i WHERE i.expirationDate < :now")
    void deleteExpiredInviteCodesBefore(@Param("now") LocalDateTime now); // 만료된 초대 코드를 삭제
}
