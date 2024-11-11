package com.example.toychat.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ChatRoomListResponseDTO {

    @JsonProperty("chatroom_id")
    private Long chatroomId;

    private String title;

    @JsonProperty("max_members")
    private int maxMembers;

    @JsonProperty("is_private")
    private boolean isPrivate;

    @JsonProperty("current_members")
    private int currentMembers;  // 현재 참여 인원 수
}
