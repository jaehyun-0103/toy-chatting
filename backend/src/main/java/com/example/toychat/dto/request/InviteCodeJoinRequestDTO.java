package com.example.toychat.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InviteCodeJoinRequestDTO {

    @JsonProperty("invite_code")
    private String inviteCode;
}
