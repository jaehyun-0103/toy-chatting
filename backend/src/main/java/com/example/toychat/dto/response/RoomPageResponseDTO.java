package com.example.toychat.dto.response;

import java.util.List;

public record RoomPageResponseDTO(
        List<ChatRoomListResponseDTO> items,
        boolean hasNext,
        List<Long> joinedRoomIds
) {
}