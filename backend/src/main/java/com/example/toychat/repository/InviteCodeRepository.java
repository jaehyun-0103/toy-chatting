package com.example.toychat.repository;

import com.example.toychat.entity.ChatRoom;
import com.example.toychat.entity.InviteCode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface InviteCodeRepository extends JpaRepository<InviteCode, Long> {
    Optional<InviteCode> findByChatRoom(ChatRoom chatRoom); // 주어진 채팅방에 대한 초대 코드를 조회
    Optional<InviteCode> findByInviteCode(String inviteCode); // 주어진 초대 코드를 통해 초대 코드 정보를 조회
}
