package com.example.toychat.repository;

import com.example.toychat.entity.ChatRoom;
import com.example.toychat.entity.ChatRoomMember;
import com.example.toychat.entity.User;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ChatRoomMemberRepository extends JpaRepository<ChatRoomMember, Long> {
    // 채팅방에 사용자 참여 여부 확인
    boolean existsByChatRoomAndUser(ChatRoom chatRoom, User user);

    // 채팅방 ID와 사용자 ID로 채팅방 참여 여부 확인
    boolean existsByChatRoom_IdAndUser_Id(Long chatRoomId, Long userId);

    // 채팅방에 참여한 인원 수 계산
    int countByChatRoom(ChatRoom chatRoom);

    // 사용자가 참여한 채팅방 목록 조회
    @Query("SELECT cr FROM ChatRoom cr JOIN cr.members crm WHERE crm.user = :user")
    List<ChatRoom> findChatRoomsByUser(User user);

    // 채팅방의 멤버 목록 조회
    List<ChatRoomMember> findByChatRoom(ChatRoom chatRoom);

    // 채팅방에 참여한 회원 삭제
    void deleteByChatRoomAndUser(ChatRoom chatRoom, User user);

    // 특정 사용자의 모든 채팅방 참여 정보 삭제
    void deleteByUser(User user);

    // 주어진 채팅방 ID 목록 중 사용자가 참여한 채팅방 ID 목록 조회
    @Query("""
        SELECT crm.chatRoom.id
        FROM ChatRoomMember crm
        WHERE crm.user.id = :userId
          AND crm.chatRoom.id IN (:roomIds)
        """)
    List<Long> findJoinedRoomIdsInPage(
            @Param("userId") Long userId,
            @Param("roomIds") List<Long> roomIds
    );
}