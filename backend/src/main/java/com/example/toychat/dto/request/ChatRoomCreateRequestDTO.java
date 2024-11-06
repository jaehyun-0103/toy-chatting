package com.example.toychat.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ChatRoomCreateRequestDTO {

    private String title;

    @JsonProperty("max_members")
    private int maxMembers;

    @JsonProperty("is_private")
    private boolean isPrivate;
}
