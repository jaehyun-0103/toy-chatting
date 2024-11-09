package com.example.toychat.repository;

import com.example.toychat.entity.ChatRoom;
import com.example.toychat.entity.ChatRoomMember;
import com.example.toychat.entity.User;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ChatRoomMemberRepository extends JpaRepository<ChatRoomMember, Long> {
    Optional<ChatRoomMember> findByChatRoomAndUser(ChatRoom chatRoom, User user);

    int countByChatRoom(ChatRoom chatRoom); // 특정 채팅방에 참여한 인원 수 계산

    boolean existsByChatRoomAndUser(ChatRoom chatRoom, User user); // 사용자 존재 여부 확인

    @Query("SELECT cr FROM ChatRoom cr JOIN cr.members crm WHERE crm.user = :user")
    List<ChatRoom> findChatRoomsByUser(User user); // 사용자가 참여한 채팅방 목록 조회

    List<ChatRoomMember> findByChatRoom(ChatRoom chatRoom); // 특정 채팅방의 멤버 목록 조회

    void deleteByChatRoomAndUser(ChatRoom chatRoom, User user); // 특정 채팅방과 사용자로 회원 삭제
}