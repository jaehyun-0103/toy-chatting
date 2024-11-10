package com.example.toychat.repository;

import com.example.toychat.entity.ChatRoom;
import com.example.toychat.entity.ChatRoomMember;
import com.example.toychat.entity.User;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ChatRoomMemberRepository extends JpaRepository<ChatRoomMember, Long> {
    // 채팅방에 사용자 참여 여부 확인
    boolean existsByChatRoomAndUser(ChatRoom chatRoom, User user);

    // 채팅방에 참여한 인원 수 계산
    int countByChatRoom(ChatRoom chatRoom);

    // 사용자가 참여한 채팅방 목록 조회
    @Query("SELECT cr FROM ChatRoom cr JOIN cr.members crm WHERE crm.user = :user")
    List<ChatRoom> findChatRoomsByUser(User user);

    // 채팅방의 멤버 목록 조회
    List<ChatRoomMember> findByChatRoom(ChatRoom chatRoom);

    // 채팅방에 참여한 회원 삭제
    void deleteByChatRoomAndUser(ChatRoom chatRoom, User user);
}