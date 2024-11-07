package com.example.toychat.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ChatRoomMemberResponseDTO {

    @JsonProperty("user_id")
    private Long userId;

    @JsonProperty("username")
    private String username;

    @JsonProperty("joined_at")
    private LocalDateTime joinedAt;
}