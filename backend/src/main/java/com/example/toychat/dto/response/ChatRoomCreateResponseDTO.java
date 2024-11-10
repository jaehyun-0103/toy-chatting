package com.example.toychat.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class ChatRoomCreateResponseDTO {

    private String message;

    @JsonProperty("chatroom_id")
    private Long chatroomId;

    @JsonProperty("creator_id")
    private Long creatorId;
}
