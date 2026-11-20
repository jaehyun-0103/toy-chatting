package com.example.toychat.repository;

import com.example.toychat.dto.response.ChatRoomListResponseDTO;
import com.example.toychat.entity.ChatRoom;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {

    // 생성자 ID로 채팅방 존재 여부 확인
    boolean existsByCreator_Id(Long creatorId);

    // 비관적 락을 사용하여 채팅방 ID로 채팅방 조회
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM ChatRoom c WHERE c.id = :id")
    Optional<ChatRoom> findByIdWithLock(@Param("id") Long id);

    // 공개 채팅방 목록 요약 정보 조회
    @Query("""
            SELECT c.id
            FROM ChatRoom c
            WHERE c.isPrivate = false
            ORDER BY c.id DESC
            """)
    Slice<Long> findPublicRoomIds(Pageable pageable);

    // 사용자가 참여한 채팅방 ID 목록을 내림차순으로 조회
    @Query("""
            SELECT c.id
            FROM ChatRoom c
            WHERE EXISTS (
                SELECT crm.id
                FROM ChatRoomMember crm
                WHERE crm.chatRoom = c
                  AND crm.user.id = :userId
            )
            ORDER BY c.id DESC
            """)
    Slice<Long> findJoinedRoomIds(
            @Param("userId") Long userId,
            Pageable pageable
    );

    // 채팅방 ID 목록으로 채팅방 요약 정보 조회
    @Query("""
            SELECT new com.example.toychat.dto.response.ChatRoomListResponseDTO(
                c.id,
                c.creator.id,
                c.title,
                c.maxMembers,
                c.isPrivate,
                COUNT(m.id)
            )
            FROM ChatRoom c
            LEFT JOIN c.members m
            WHERE c.id IN (:roomIds)
            GROUP BY c.id, c.creator.id, c.title, c.maxMembers, c.isPrivate
            ORDER BY c.id DESC
            """)
    List<ChatRoomListResponseDTO> findRoomSummaries(
            @Param("roomIds") List<Long> roomIds
    );
}
