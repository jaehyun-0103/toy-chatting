package com.example.toychat.dto.response;

import java.util.List;

public record CursorPageResponseDTO<T>(
        List<T> items,
        Long nextCursor,
        boolean hasNext
) {
}