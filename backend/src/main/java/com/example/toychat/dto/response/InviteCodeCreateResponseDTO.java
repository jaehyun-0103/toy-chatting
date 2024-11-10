package com.example.toychat.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InviteCodeCreateResponseDTO {

    private String message;

    @JsonProperty("invite_code")
    private String inviteCode;
}
